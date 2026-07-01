package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvAirTicketDTO;
import com.asg.spindleserp.travel.dto.TrvHotelBookingDTO;
import com.asg.spindleserp.travel.dto.TrvSupplierCostDTO;
import com.asg.spindleserp.travel.service.TravelOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelOperationsController — fulfillment side: hotel bookings, air tickets,
 * supplier costs. JS prefixes: hb* (hotel booking), at* (air ticket), sc* (supplier cost).
 */
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelOperationsController {

    private final TravelOperationsService opsService;

    // ── PAGES ─────────────────────────────────────────────────────────────────

    @GetMapping("/hotel-bookings")
    public String hotelBookingsPage(Model model) {
        model.addAttribute("activePage", "travel-hotel-bookings");
        model.addAttribute("pageTitle",  "Hotel Bookings");
        return "travel/travel-hotel-bookings";
    }

    @GetMapping("/air-tickets")
    public String airTicketsPage(Model model) {
        model.addAttribute("activePage", "travel-air-tickets");
        model.addAttribute("pageTitle",  "Air Tickets");
        return "travel/travel-air-tickets";
    }

    @GetMapping("/supplier-costs")
    public String supplierCostsPage(Model model) {
        model.addAttribute("activePage", "travel-supplier-costs");
        model.addAttribute("pageTitle",  "Supplier Costs");
        return "travel/travel-supplier-costs";
    }

    // ── HOTEL BOOKINGS ────────────────────────────────────────────────────────

    @GetMapping("/hotel-bookings/list")
    @ResponseBody
    public Map<String, Object> hbList(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", opsService.listHotelBookings(search));
    }

    @GetMapping("/hotel-bookings/show/{id}")
    @ResponseBody
    public Map<String, Object> hbShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", opsService.findHotelBookingById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/hotel-bookings/save")
    @ResponseBody
    public Map<String, Object> hbSave(@RequestBody @Valid TrvHotelBookingDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvHotelBookingDTO saved = opsService.saveHotelBooking(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Hotel booking updated." : "Hotel booking created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/hotel-bookings/confirm/{id}")
    @ResponseBody
    public Map<String, Object> hbConfirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { opsService.changeHotelBookingStatus(id, "CONFIRMED"); res.put("success", true); res.put("message", "Hotel booking confirmed."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/hotel-bookings/delete/{id}")
    @ResponseBody
    public Map<String, Object> hbDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { opsService.deleteHotelBooking(id); res.put("success", true); res.put("message", "Hotel booking deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AIR TICKETS ───────────────────────────────────────────────────────────

    @GetMapping("/air-tickets/list")
    @ResponseBody
    public Map<String, Object> atList(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", opsService.listAirTickets(search));
    }

    @GetMapping("/air-tickets/show/{id}")
    @ResponseBody
    public Map<String, Object> atShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", opsService.findAirTicketById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/air-tickets/save")
    @ResponseBody
    public Map<String, Object> atSave(@RequestBody @Valid TrvAirTicketDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvAirTicketDTO saved = opsService.saveAirTicket(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Air ticket updated." : "Air ticket issued.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/air-tickets/delete/{id}")
    @ResponseBody
    public Map<String, Object> atDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { opsService.deleteAirTicket(id); res.put("success", true); res.put("message", "Air ticket deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── SUPPLIER COSTS ────────────────────────────────────────────────────────

    @GetMapping("/supplier-costs/list")
    @ResponseBody
    public Map<String, Object> scList(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", opsService.listSupplierCosts(search));
    }

    @PostMapping("/supplier-costs/save")
    @ResponseBody
    public Map<String, Object> scSave(@RequestBody @Valid TrvSupplierCostDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvSupplierCostDTO saved = opsService.saveSupplierCost(dto);
            res.put("success", true); res.put("id", saved.getId()); res.put("message", "Supplier cost saved.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/supplier-costs/delete/{id}")
    @ResponseBody
    public Map<String, Object> scDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { opsService.deleteSupplierCost(id); res.put("success", true); res.put("message", "Supplier cost deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── LOOKUPS (Select2) ─────────────────────────────────────────────────────

    @GetMapping("/service-lines/search")
    @ResponseBody
    public Map<String, Object> serviceLineSearch(@RequestParam String serviceType) {
        return Map.of("items", opsService.unfulfilledServiceLines(serviceType));
    }

    @GetMapping("/service-lines/{id}/passengers")
    @ResponseBody
    public Map<String, Object> passengersForLine(@PathVariable Long id) {
        return Map.of("items", opsService.passengersForServiceLine(id));
    }
}
