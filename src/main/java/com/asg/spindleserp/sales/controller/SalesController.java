package com.asg.spindleserp.sales.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.sales.dto.SalesDocumentDTO;
import com.asg.spindleserp.sales.service.SalesServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SalesController — one controller for the full sales cycle.
 *
 * Pages:
 *   GET /sales/orders         → sales-orders.html
 *   GET /sales/deliveries     → sales-deliveries.html
 *   GET /sales/invoices       → sales-invoices.html
 *   GET /sales/credit-notes   → sales-credit-notes.html
 *
 * Shared REST:
 *   GET    /sales/docs/list?documentType=SALES_ORDER
 *   GET    /sales/docs/show/{id}
 *   POST   /sales/docs/save
 *   POST   /sales/docs/confirm/{id}
 *   POST   /sales/docs/cancel/{id}
 *   DELETE /sales/docs/delete/{id}
 *   GET    /sales/docs/populate?parentId=&childType=
 *   GET    /sales/docs/open-sos?customerId=
 *   GET    /sales/docs/confirmed-deliveries?customerId=
 *
 * JS prefix: so / dn / si / cn
 * Cascade functions (DataTable action buttons):
 *   createDeliveryFromSO(id)       → populate DELIVERY_ORDER from SO
 *   createInvoiceFromDelivery(id)  → populate SALES_INVOICE from Delivery
 *   createReceiptFromInvoice(id)   → navigate to receipt-vouchers pre-filled
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SalesController {

    private final SalesServiceImpl salesService;

    // ── Pages ─────────────────────────────────────────────────────────────────

    @GetMapping("/sales/orders")
    public String soPage(Model model) {
        model.addAttribute("activePage", "sales-orders");
        model.addAttribute("documentType", "SALES_ORDER");
        model.addAttribute("pageTitle", "Sales Orders");
        return "sales/sales-orders";
    }

    @GetMapping("/sales/deliveries")
    public String deliveryPage(Model model) {
        model.addAttribute("activePage", "sales-deliveries");
        model.addAttribute("documentType", "DELIVERY_ORDER");
        model.addAttribute("pageTitle", "Delivery Notes");
        return "sales/sales-deliveries";
    }

    @GetMapping("/sales/invoices")
    public String invoicePage(Model model) {
        model.addAttribute("activePage", "sales-invoices");
        model.addAttribute("documentType", "SALES_INVOICE");
        model.addAttribute("pageTitle", "Sales Invoices");
        return "sales/sales-invoices";
    }

    @GetMapping("/sales/credit-notes")
    public String creditNotePage(Model model) {
        model.addAttribute("activePage", "sales-credit-notes");
        model.addAttribute("documentType", "CREDIT_NOTE");
        model.addAttribute("pageTitle", "Credit Notes");
        return "sales/sales-credit-notes";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/sales/docs/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "SALES_ORDER") String documentType) {
        return salesService.datatableList(documentType, draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/sales/docs/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", salesService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @PostMapping("/sales/docs/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid SalesDocumentDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            SalesDocumentDTO saved = salesService.save(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("documentNo", saved.getDocumentNo());
            res.put("message", dto.getId() != null ? "Document updated." : "Document saved as draft.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

    @PostMapping("/sales/docs/confirm/{id}")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            SalesDocumentDTO dto = salesService.confirm(id);
            res.put("success", true);
            res.put("message", "Document " + dto.getDocumentNo() + " confirmed successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @PostMapping("/sales/docs/cancel/{id}")
    @ResponseBody
    public Map<String, Object> cancel(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            SalesDocumentDTO dto = salesService.cancel(id);
            res.put("success", true); res.put("message", "Document " + dto.getDocumentNo() + " cancelled.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/sales/docs/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { salesService.delete(id); res.put("success", true); res.put("message", "Document deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Cascade populate ──────────────────────────────────────────────────────

    @GetMapping("/sales/docs/populate")
    @ResponseBody
    public Map<String, Object> populate(@RequestParam Long parentId,
                                         @RequestParam String childType) {
        Map<String, Object> res = new HashMap<>();
        try {
            SalesDocumentDTO dto = salesService.populateFromSource(parentId, childType);
            res.put("success", true); res.put("obj", Map.of("defaultData", dto));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AJAX lookups ──────────────────────────────────────────────────────────

    @GetMapping("/sales/docs/open-sos")
    @ResponseBody
    public List<Map<String, Object>> openSOs(@RequestParam Long customerId) {
        return salesService.openSOsForCustomer(customerId);
    }

    @GetMapping("/sales/docs/confirmed-deliveries")
    @ResponseBody
    public List<Map<String, Object>> confirmedDeliveries(@RequestParam Long customerId) {
        return salesService.confirmedDeliveriesForCustomer(customerId);
    }
}
