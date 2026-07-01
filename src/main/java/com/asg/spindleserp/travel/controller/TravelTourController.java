package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvTourBookingDTO;
import com.asg.spindleserp.travel.dto.TrvTourDTO;
import com.asg.spindleserp.travel.dto.TrvTourGuideDTO;
import com.asg.spindleserp.travel.service.TravelTourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelTourController — tours, tour guides, and tour bookings.
 * JS prefixes: tour* (tour), guide* (tour guide), tourbk* (tour booking).
 */
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelTourController {

    private final TravelTourService tourService;

    @GetMapping("/tours")
    public String toursPage(Model model) {
        model.addAttribute("activePage", "travel-tours");
        model.addAttribute("pageTitle",  "Tours");
        return "travel/travel-tours";
    }

    // ── TOURS ─────────────────────────────────────────────────────────────────

    @GetMapping("/tours/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", tourService.listTours(search));
    }

    @GetMapping("/tours/search")
    @ResponseBody
    public Map<String, Object> search(@RequestParam(defaultValue = "") String search) {
        return Map.of("items", tourService.searchTours(search));
    }

    @GetMapping("/tours/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", tourService.findTourById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/tours/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid TrvTourDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvTourDTO saved = tourService.saveTour(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Tour updated." : "Tour created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/tours/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { tourService.deleteTour(id); res.put("success", true); res.put("message", "Tour deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── TOUR GUIDES ───────────────────────────────────────────────────────────

    @GetMapping("/tours/guides/list")
    @ResponseBody
    public Map<String, Object> guideList() { return Map.of("data", tourService.listGuides()); }

    @GetMapping("/tours/guides/search")
    @ResponseBody
    public Map<String, Object> guideSearch() { return Map.of("items", tourService.searchGuides()); }

    @PostMapping("/tours/guides/save")
    @ResponseBody
    public Map<String, Object> guideSave(@RequestBody @Valid TrvTourGuideDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { tourService.saveGuide(dto); res.put("success", true); res.put("message", "Guide saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/tours/guides/delete/{id}")
    @ResponseBody
    public Map<String, Object> guideDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { tourService.deleteGuide(id); res.put("success", true); res.put("message", "Guide deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── TOUR BOOKINGS ─────────────────────────────────────────────────────────

    @GetMapping("/tour-bookings/list")
    @ResponseBody
    public Map<String, Object> bookingList(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", tourService.listTourBookings(search));
    }

    @GetMapping("/tour-bookings/show/{id}")
    @ResponseBody
    public Map<String, Object> bookingShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", tourService.findTourBookingById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/tour-bookings/save")
    @ResponseBody
    public Map<String, Object> bookingSave(@RequestBody @Valid TrvTourBookingDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvTourBookingDTO saved = tourService.saveTourBooking(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Tour booking updated." : "Tour booking created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/tour-bookings/confirm/{id}")
    @ResponseBody
    public Map<String, Object> bookingConfirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { tourService.changeTourBookingStatus(id, "CONFIRMED"); res.put("success", true); res.put("message", "Tour booking confirmed."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/tour-bookings/delete/{id}")
    @ResponseBody
    public Map<String, Object> bookingDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { tourService.deleteTourBooking(id); res.put("success", true); res.put("message", "Tour booking deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
