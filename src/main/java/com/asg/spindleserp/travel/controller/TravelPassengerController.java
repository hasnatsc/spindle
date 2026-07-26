package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.PassportScanDTO;
import com.asg.spindleserp.travel.dto.TrvPassengerDTO;
import com.asg.spindleserp.travel.service.TravelPassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelPassengerController — passenger master with passport reading.
 * JS prefix: pg*
 *
 * Page:
 *   GET    /travel/passengers
 *
 * CRUD:
 *   GET    /travel/passengers/list?search=&bookingId=&expiryFilter=
 *   GET    /travel/passengers/show/{id}
 *   POST   /travel/passengers/save
 *   POST   /travel/passengers/set-lead/{id}
 *   DELETE /travel/passengers/delete/{id}
 *
 * Lookups:
 *   GET    /travel/passengers/booking-search?q=
 *   GET    /travel/service-lines/{id}/passengers        (GET already exists in
 *                                                        TravelOperationsController)
 *   POST   /travel/service-lines/{id}/passengers        (NEW — the Air Tickets
 *                                                        "New Passenger" modal
 *                                                        was already calling
 *                                                        this; nothing served it)
 *
 * Passport:
 *   GET    /travel/passengers/passport/capabilities
 *   POST   /travel/passengers/passport/parse    { "mrzText": "..." }
 *   POST   /travel/passengers/passport/scan     (multipart, needs a
 *                                                PassportOcrEngine bean)
 *   POST   /travel/passengers/{id}/passport-image (multipart)
 *
 * URL security: /travel/** is already bound to the TRAVEL module by
 * DynamicAuthorizationManager, so every route below inherits the module gate
 * with no extra seed rows.
 */
@Slf4j
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelPassengerController {

    private final TravelPassengerService passengerService;

    // ── PAGE ──────────────────────────────────────────────────────────────────

    @GetMapping("/passengers")
    public String passengersPage(Model model) {
        model.addAttribute("activePage", "travel-passengers");
        model.addAttribute("pageTitle",  "Passengers");
        model.addAttribute("serverOcr",  passengerService.serverOcrAvailable());
        return "travel/travel-passengers";
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    @GetMapping("/passengers/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam(defaultValue = "") String search,
                                    @RequestParam(required = false) Long bookingId,
                                    @RequestParam(defaultValue = "") String expiryFilter) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("data", passengerService.list(search, bookingId, expiryFilter));
            res.put("success", true);
        } catch (Exception e) {
            log.error("Passenger list failed", e);
            res.put("success", false);
            res.put("data", java.util.List.of());
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── SHOW ──────────────────────────────────────────────────────────────────

    @GetMapping("/passengers/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", passengerService.findById(id)));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    @PostMapping("/passengers/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid TrvPassengerDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            boolean isUpdate = dto.getId() != null;
            TrvPassengerDTO saved = passengerService.save(dto);
            res.put("success", true);
            res.put("id", saved.getId());
            res.put("passenger", saved);
            res.put("obj", Map.of("defaultData", saved));
            res.put("message", isUpdate
                ? saved.getFullName() + " updated."
                : saved.getFullName() + " added to booking " + saved.getBookingNo() + ".");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── SET LEAD ──────────────────────────────────────────────────────────────

    @PostMapping("/passengers/set-lead/{id}")
    @ResponseBody
    public Map<String, Object> setLead(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvPassengerDTO saved = passengerService.setLeadPassenger(id);
            res.put("success", true);
            res.put("message", saved.getFullName() + " is now the lead passenger.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/passengers/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            passengerService.delete(id);
            res.put("success", true);
            res.put("message", "Passenger deleted.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── BOOKING LOOKUP ────────────────────────────────────────────────────────

    @GetMapping("/passengers/booking-search")
    @ResponseBody
    public Map<String, Object> bookingSearch(@RequestParam(defaultValue = "") String q) {
        return Map.of("items", passengerService.bookingSearch(q));
    }

    // ── CREATE FROM A SERVICE LINE (Air Tickets modal) ────────────────────────

    /**
     * The New Passenger modal on travel-air-tickets.html has always POSTed
     * here — TravelOperationsController only ever mapped the GET, so the save
     * silently 405'd. This is the missing half.
     */
    @PostMapping("/service-lines/{bookingServiceId}/passengers")
    @ResponseBody
    public Map<String, Object> createForServiceLine(@PathVariable Long bookingServiceId,
                                                    @RequestBody @Valid TrvPassengerDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvPassengerDTO saved = passengerService.createForServiceLine(bookingServiceId, dto);
            res.put("success", true);
            res.put("passenger", saved);
            res.put("message", saved.getFullName() + " added.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // =========================================================================
    // PASSPORT
    // =========================================================================

    /** Lets the browser decide whether to offer the server-scan button. */
    @GetMapping("/passengers/passport/capabilities")
    @ResponseBody
    public Map<String, Object> capabilities() {
        return Map.of("success", true, "serverOcr", passengerService.serverOcrAvailable());
    }

    /**
     * The main path. Browser OCR (or a human) produces MRZ text; the server
     * validates check digits and returns clean, typed fields.
     */
    @PostMapping("/passengers/passport/parse")
    @ResponseBody
    public Map<String, Object> parseMrz(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String text = body.getOrDefault("mrzText", body.get("text"));
            PassportScanDTO scan = passengerService.parseMrz(text);
            if (body.get("source") != null) scan.setSource(body.get("source"));
            res.put("success", scan.isSuccess());
            res.put("message", scan.getMessage());
            res.put("obj", Map.of("defaultData", scan));
            res.put("data", scan);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    /** Optional server-side OCR — inert until a PassportOcrEngine bean exists. */
    @PostMapping(value = "/passengers/passport/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Map<String, Object> scan(@RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            PassportScanDTO scan = passengerService.scanImage(file);
            res.put("success", scan.isSuccess());
            res.put("message", scan.getMessage());
            res.put("obj", Map.of("defaultData", scan));
            res.put("data", scan);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    /** Files the passport image against the passenger as a PASSPORT document. */
    @PostMapping(value = "/passengers/{id}/passport-image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Map<String, Object> uploadPassportImage(@PathVariable Long id,
                                                   @RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long docId = passengerService.attachPassportImage(id, file);
            res.put("success", true);
            res.put("documentId", docId);
            res.put("message", "Passport image saved to this passenger.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }
}
