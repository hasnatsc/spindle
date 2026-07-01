package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.travel.dto.TrvBookingDTO;
import com.asg.spindleserp.travel.service.TravelBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelBookingController — booking cycle + dashboard.
 *
 * Pages:
 *   GET /travel/dashboard  → travel-dashboard.html
 *   GET /travel/bookings   → travel-bookings.html
 *
 * REST:
 *   GET    /travel/bookings/list
 *   GET    /travel/bookings/show/{id}
 *   POST   /travel/bookings/save
 *   POST   /travel/bookings/confirm/{id}
 *   POST   /travel/bookings/cancel/{id}
 *   DELETE /travel/bookings/delete/{id}
 *   GET    /travel/bookings/receipt-prefill?bookingId=
 *
 * Booking → Receipt Voucher bridge (mirrors Sales Invoice → Receipt):
 *   1. bkgConfirm(id) → POST /travel/bookings/confirm/{id} → SALES_VOUCHER JEM created
 *   2. createReceiptFromBooking(id) (💵 button on CONFIRMED rows with due > 0)
 *      → GET /travel/bookings/receipt-prefill?bookingId={id}
 *      → sessionStorage.rvPrefill = JSON.stringify(dto)
 *      → redirect /accounts/receipt-vouchers → rvOpenCreate(data)
 */
@Slf4j
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelBookingController {

    private final TravelBookingService bookingService;

    // ── PAGES ─────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("activePage", "travel-dashboard");
        model.addAttribute("pageTitle",  "Travel Dashboard");
        return "travel/travel-dashboard";
    }

    @GetMapping("/bookings")
    public String bookingsPage(Model model) {
        model.addAttribute("activePage", "travel-bookings");
        model.addAttribute("pageTitle",  "Bookings");
        return "travel/travel-bookings";
    }

    // ── DASHBOARD SUMMARY ────────────────────────────────────────────────────

    @GetMapping("/dashboard/summary")
    @ResponseBody
    public Map<String, Object> dashboardSummary() {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("data", bookingService.dashboardSummary()); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @GetMapping("/bookings/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status) {
        return bookingService.datatableList(draw, start, length, search, status);
    }

    // ── SHOW ──────────────────────────────────────────────────────────────────

    @GetMapping("/bookings/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", bookingService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    @PostMapping("/bookings/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid TrvBookingDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvBookingDTO saved = bookingService.save(dto);
            res.put("success",   true);
            res.put("id",        saved.getId());
            res.put("bookingNo", saved.getBookingNo());
            res.put("message",   dto.getId() != null ? "Booking updated." : "Booking saved as draft.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── CONFIRM ───────────────────────────────────────────────────────────────

    @PostMapping("/bookings/confirm/{id}")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvBookingDTO dto = bookingService.confirm(id);
            res.put("success", true);
            res.put("message", "Booking " + dto.getBookingNo() + " confirmed successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────

    @PostMapping("/bookings/cancel/{id}")
    @ResponseBody
    public Map<String, Object> cancel(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvBookingDTO dto = bookingService.cancel(id);
            res.put("success", true);
            res.put("message", "Booking " + dto.getBookingNo() + " cancelled.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/bookings/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { bookingService.delete(id); res.put("success", true); res.put("message", "Booking deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── RECEIPT PREFILL  Booking → Receipt Voucher ───────────────────────────

    @GetMapping("/bookings/receipt-prefill")
    @ResponseBody
    public Map<String, Object> receiptPrefill(@RequestParam Long bookingId) {
        Map<String, Object> res = new HashMap<>();
        try {
            VoucherDTO dto = bookingService.populateReceiptFromBooking(bookingId);
            res.put("success",    true);
            res.put("redirectTo", "/accounts/receipt-vouchers");
            res.put("obj",        Map.of("defaultData", dto));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
