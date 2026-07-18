// Path: com/asg/spindleserp/travel/controller/TravelPortalController.java
package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvBookingDTO;
import com.asg.spindleserp.travel.dto.TrvPackageDTO;
import com.asg.spindleserp.travel.dto.TrvTourDTO;
import com.asg.spindleserp.travel.service.TravelBookingService;
import com.asg.spindleserp.travel.service.TravelPackageService;
import com.asg.spindleserp.travel.service.TravelTourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TravelPortalController — the public, no-login-required customer site.
 * Browse packages/tours, then submit an enquiry that lands as a DRAFT
 * TrvBooking — an agent picks it up from the existing /travel/bookings
 * queue and takes it from there (quote → confirm → fulfilment), exactly
 * the same booking pipeline the front desk and back office already use.
 *
 * No customer accounts, no payment gateway — this is a lead-capture +
 * enquiry funnel, matching how travel agencies actually convert web
 * traffic (call/WhatsApp follow-up), not a self-checkout flow.
 *
 * Pages:
 *   GET /travel-site                     home — featured packages + tours
 *   GET /travel-site/packages            all packages (+ ?q= search)
 *   GET /travel-site/packages/{id}       package detail + enquiry form
 *   GET /travel-site/tours                all tours (+ ?q= search)
 *   GET /travel-site/tours/{id}          tour detail + enquiry form
 *   GET /travel-site/enquiry/success/{bookingNo}
 *
 * REST:
 *   POST /travel-site/enquire            {type, referenceId, paxCount,
 *                                          fullName, phone, email, message}
 *                                         → {success, bookingNo}
 *
 * ★ Multi-tenant resolution — this controller assumes ContextProvider can
 *   resolve organizationId for an anonymous request the same way the
 *   eCommerce storefront does (subdomain/host binding). No Spring Security
 *   auth is required; permitAll this whole /travel-site/** prefix.
 */
@Slf4j
@Controller
@RequestMapping("/travel-site")
@RequiredArgsConstructor
public class TravelPortalController {

    private final TravelPackageService packageService;
    private final TravelTourService tourService;
    private final TravelBookingService bookingService;

    private static final int FEATURED_LIMIT = 6;

    // ── HOME ─────────────────────────────────────────────────────────────────
    @GetMapping
    public String home(Model model) {
        model.addAttribute("featuredPackages", limit(packageService.findAllActive(), FEATURED_LIMIT));
        model.addAttribute("featuredTours", limit(tourService.findAllActive(), FEATURED_LIMIT));
        return "travel-site/tf-home";
    }

    // ── PACKAGES ─────────────────────────────────────────────────────────────
    @GetMapping("/packages")
    public String packages(@RequestParam(required = false) String q, Model model) {
        List<TrvPackageDTO> items = (q != null && !q.isBlank())
                ? packageService.findAllActive().stream()
                    .filter(p -> p.getPackageName() != null
                        && p.getPackageName().toLowerCase().contains(q.toLowerCase()))
                    .collect(Collectors.toList())
                : packageService.findAllActive();
        model.addAttribute("packages", items);
        model.addAttribute("query", q);
        return "travel-site/tf-packages";
    }

    @GetMapping("/packages/{id}")
    public String packageDetail(@PathVariable Long id, Model model) {
        try {
            TrvPackageDTO pkg = packageService.findPackageById(id);
            if (pkg == null || pkg.getIsActive() == null || !pkg.getIsActive())
                return "redirect:/travel-site/packages";
            model.addAttribute("pkg", pkg);
            return "travel-site/tf-package-detail";
        } catch (Exception e) {
            return "redirect:/travel-site/packages";
        }
    }

    // ── TOURS ────────────────────────────────────────────────────────────────
    @GetMapping("/tours")
    public String tours(@RequestParam(required = false) String q, Model model) {
        List<TrvTourDTO> items = (q != null && !q.isBlank())
                ? tourService.findAllActive().stream()
                    .filter(t -> t.getTourName() != null
                        && t.getTourName().toLowerCase().contains(q.toLowerCase()))
                    .collect(Collectors.toList())
                : tourService.findAllActive();
        model.addAttribute("tours", items);
        model.addAttribute("query", q);
        return "travel-site/tf-tours";
    }

    @GetMapping("/tours/{id}")
    public String tourDetail(@PathVariable Long id, Model model) {
        try {
            TrvTourDTO tour = tourService.findTourById(id);
            if (tour == null || tour.getIsActive() == null || !tour.getIsActive())
                return "redirect:/travel-site/tours";
            model.addAttribute("tour", tour);
            return "travel-site/tf-tour-detail";
        } catch (Exception e) {
            return "redirect:/travel-site/tours";
        }
    }

    // ── ENQUIRY ──────────────────────────────────────────────────────────────
    @PostMapping("/enquire")
    @ResponseBody
    public Map<String, Object> enquire(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String type = String.valueOf(body.get("type"));           // "PACKAGE" | "TOUR"
            Long referenceId = Long.valueOf(body.get("referenceId").toString());
            int paxCount = body.get("paxCount") != null ? Integer.parseInt(body.get("paxCount").toString()) : 1;
            String fullName = str(body.get("fullName"));
            String phone = str(body.get("phone"));
            String email = str(body.get("email"));
            String message = str(body.get("message"));

            if (fullName == null || fullName.isBlank())
                throw new IllegalArgumentException("Please enter your name.");
            if (phone == null || phone.isBlank())
                throw new IllegalArgumentException("Please enter a contact phone number.");

            BigDecimal unitPrice = BigDecimal.ZERO;
            String description;
            if ("TOUR".equalsIgnoreCase(type)) {
                TrvTourDTO tour = tourService.findTourById(referenceId);
                unitPrice = tour.getBasePrice() != null ? tour.getBasePrice() : BigDecimal.ZERO;
                description = tour.getTourName();
            } else {
                TrvPackageDTO pkg = packageService.findPackageById(referenceId);
                unitPrice = pkg.getBasePrice() != null ? pkg.getBasePrice() : BigDecimal.ZERO;
                description = pkg.getPackageName();
            }
            BigDecimal qty = BigDecimal.valueOf(Math.max(1, paxCount));
            BigDecimal lineTotal = unitPrice.multiply(qty);

            List<String> nameParts = splitName(fullName);

            TrvBookingDTO.ServiceLineDTO line = TrvBookingDTO.ServiceLineDTO.builder()
                    .serviceType("TOUR".equalsIgnoreCase(type) ? "TOUR" : "PACKAGE")
                    .referenceId(referenceId)
                    .description(description)
                    .quantity(qty)
                    .unitCost(BigDecimal.ZERO)
                    .unitPrice(unitPrice)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .lineTotal(lineTotal)
                    .build();

            TrvBookingDTO.PassengerDTO lead = TrvBookingDTO.PassengerDTO.builder()
                    .firstName(nameParts.get(0))
                    .lastName(nameParts.get(1))
                    .phone(phone)
                    .email(email)
                    .passengerType("ADULT")
                    .isLeadPassenger(true)
                    .build();

            List<TrvBookingDTO.ServiceLineDTO> services = new ArrayList<>();
            services.add(line);
            List<TrvBookingDTO.PassengerDTO> passengers = new ArrayList<>();
            passengers.add(lead);

            TrvBookingDTO booking = TrvBookingDTO.builder()
                    .bookingType("TOUR".equalsIgnoreCase(type) ? "PACKAGE" : "PACKAGE") // schema allows PACKAGE|HOTEL|AIR|COMBINED
                    .bookingDate(LocalDate.now())
                    .status("DRAFT")
                    .currency("BDT")
                    .exchangeRate(BigDecimal.ONE)
                    .subtotalAmount(lineTotal)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .totalAmount(lineTotal)
                    .paidAmount(BigDecimal.ZERO)
                    .dueAmount(lineTotal)
                    .remarks("Website enquiry" + (message != null && !message.isBlank() ? " — " + message : ""))
                    .services(services)
                    .passengers(passengers)
                    .build();

            TrvBookingDTO saved = bookingService.save(booking);
            res.put("success", true);
            res.put("bookingNo", saved.getBookingNo());
            res.put("message", "Enquiry received — reference " + saved.getBookingNo());
        } catch (Exception e) {
            log.warn("Travel enquiry failed: {}", e.getMessage());
            res.put("success", false);
            res.put("message", e.getMessage() != null ? e.getMessage() : "Could not submit your enquiry.");
        }
        return res;
    }

    @GetMapping("/enquiry/success/{bookingNo}")
    public String enquirySuccess(@PathVariable String bookingNo, Model model) {
        model.addAttribute("bookingNo", bookingNo);
        return "travel-site/tf-enquiry-success";
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private static <T> List<T> limit(List<T> list, int n) {
        return list == null ? List.of() : list.stream().limit(n).toList();
    }

    private static String str(Object o) {
        return o != null ? o.toString().trim() : null;
    }

    private static List<String> splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        List<String> out = new ArrayList<>();
        out.add(parts[0]);
        out.add(parts.length > 1 ? parts[1] : null);
        return out;
    }
}
