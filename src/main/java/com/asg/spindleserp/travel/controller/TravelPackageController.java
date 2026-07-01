package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvPackageBookingDTO;
import com.asg.spindleserp.travel.dto.TrvPackageDTO;
import com.asg.spindleserp.travel.service.TravelPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelPackageController — packages (with itinerary + inclusions) and their
 * fulfillment bookings. JS prefixes: pkg* (package), pkgbk* (package booking).
 */
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelPackageController {

    private final TravelPackageService packageService;

    @GetMapping("/packages")
    public String packagesPage(Model model) {
        model.addAttribute("activePage", "travel-packages");
        model.addAttribute("pageTitle",  "Packages");
        return "travel/travel-packages";
    }

    // ── PACKAGES ──────────────────────────────────────────────────────────────

    @GetMapping("/packages/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", packageService.listPackages(search));
    }

    @GetMapping("/packages/search")
    @ResponseBody
    public Map<String, Object> search(@RequestParam(defaultValue = "") String search) {
        return Map.of("items", packageService.searchPackages(search));
    }

    @GetMapping("/packages/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", packageService.findPackageById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/packages/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid TrvPackageDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvPackageDTO saved = packageService.savePackage(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Package updated." : "Package created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/packages/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { packageService.deletePackage(id); res.put("success", true); res.put("message", "Package deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── PACKAGE BOOKINGS ──────────────────────────────────────────────────────

    @GetMapping("/package-bookings/list")
    @ResponseBody
    public Map<String, Object> bookingList(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", packageService.listPackageBookings(search));
    }

    @GetMapping("/package-bookings/show/{id}")
    @ResponseBody
    public Map<String, Object> bookingShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", packageService.findPackageBookingById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/package-bookings/save")
    @ResponseBody
    public Map<String, Object> bookingSave(@RequestBody @Valid TrvPackageBookingDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvPackageBookingDTO saved = packageService.savePackageBooking(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Package booking updated." : "Package booking created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/package-bookings/confirm/{id}")
    @ResponseBody
    public Map<String, Object> bookingConfirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { packageService.changePackageBookingStatus(id, "CONFIRMED"); res.put("success", true); res.put("message", "Package booking confirmed."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/package-bookings/delete/{id}")
    @ResponseBody
    public Map<String, Object> bookingDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { packageService.deletePackageBooking(id); res.put("success", true); res.put("message", "Package booking deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
