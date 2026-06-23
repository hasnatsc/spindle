package com.asg.spindleserp.purchase.controller;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.purchase.dto.PurchaseDocumentDTO;
import com.asg.spindleserp.purchase.service.PurchaseDashboardService;
import com.asg.spindleserp.purchase.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService          purchaseService;
    private final PurchaseDashboardService dashboardService;

    // ── DASHBOARD ─────────────────────────────────────────────────────────────

    @GetMapping("/purchase/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("activePage", "purchase-dashboard");
        return "dashboard/purchase-dashboard";
    }

    @GetMapping("/purchase/dashboard/summary")
    @ResponseBody
    public Map<String, Object> dashboardSummary() {
        return dashboardService.summary();
    }

    // ── DOCUMENT PAGES ────────────────────────────────────────────────────────

    @GetMapping("/purchase/orders")
    public String poPage(Model model) {
        model.addAttribute("activePage",   "purchase-orders");
        model.addAttribute("documentType", "PURCHASE_ORDER");
        model.addAttribute("pageTitle",    "Purchase Orders");
        return "purchase/purchase-orders";
    }

    @GetMapping("/purchase/grns")
    public String grnPage(Model model) {
        model.addAttribute("activePage",   "purchase-grns");
        model.addAttribute("documentType", "GOODS_RECEIPT_NOTE");
        model.addAttribute("pageTitle",    "Goods Receipt Notes");
        return "purchase/purchase-grns";
    }

    @GetMapping("/purchase/invoices")
    public String invoicePage(Model model) {
        model.addAttribute("activePage",   "purchase-invoices");
        model.addAttribute("documentType", "PURCHASE_INVOICE");
        model.addAttribute("pageTitle",    "Purchase Invoices");
        return "purchase/purchase-invoices";
    }

    @GetMapping("/purchase/debit-notes")
    public String debitNotePage(Model model) {
        model.addAttribute("activePage",   "purchase-debit-notes");
        model.addAttribute("documentType", "DEBIT_NOTE");
        model.addAttribute("pageTitle",    "Debit Notes");
        return "purchase/purchase-debit-notes";
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @GetMapping("/purchase/docs/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "PURCHASE_ORDER") String documentType) {
        return purchaseService.datatableList(documentType, draw, start, length, search);
    }

    // ── SHOW ──────────────────────────────────────────────────────────────────

    @GetMapping("/purchase/docs/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", purchaseService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    @PostMapping("/purchase/docs/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid PurchaseDocumentDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            PurchaseDocumentDTO saved = purchaseService.save(dto);
            res.put("success",    true);
            res.put("id",         saved.getId());
            res.put("documentNo", saved.getDocumentNo());
            res.put("message",    dto.getId() != null ? "Document updated." : "Document saved as draft.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── CONFIRM ───────────────────────────────────────────────────────────────

    @PostMapping("/purchase/docs/confirm/{id}")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            PurchaseDocumentDTO dto = purchaseService.confirm(id);
            res.put("success", true);
            res.put("message", "Document " + dto.getDocumentNo() + " confirmed successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────

    @PostMapping("/purchase/docs/cancel/{id}")
    @ResponseBody
    public Map<String, Object> cancel(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            PurchaseDocumentDTO dto = purchaseService.cancel(id);
            res.put("success", true);
            res.put("message", "Document " + dto.getDocumentNo() + " cancelled.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/purchase/docs/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            purchaseService.delete(id);
            res.put("success", true);
            res.put("message", "Document deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── CASCADE POPULATE (PO→GRN, GRN→PI, PI→DN) ─────────────────────────────

    @GetMapping("/purchase/docs/populate")
    @ResponseBody
    public Map<String, Object> populate(@RequestParam Long parentId,
                                         @RequestParam String childType) {
        Map<String, Object> res = new HashMap<>();
        try {
            PurchaseDocumentDTO dto = purchaseService.populateFromSource(parentId, childType);
            res.put("success", true);
            res.put("obj", Map.of("defaultData", dto));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── PAYMENT PREFILL  PI → Payment Voucher (THE FIX) ──────────────────────

    /**
     * Called by createPaymentFromInvoice(piId) in purchase-invoices.html.
     *
     * Returns a pre-filled VoucherDTO. JS stores it in sessionStorage then
     * redirects to /accounts/payment-vouchers. That page reads from sessionStorage
     * and pre-fills: supplier, amount, due-date, allocation row.
     *
     * Full flow:
     *   1. User clicks 💰 on confirmed PI row  → createPaymentFromInvoice(piId)
     *   2. JS: GET /purchase/docs/payment-prefill?invoiceId={piId}
     *   3. Server returns { success, redirectTo, obj.defaultData: VoucherDTO }
     *   4. JS: sessionStorage.setItem('pvPrefill', JSON.stringify(data.obj.defaultData))
     *   5. JS: window.location.href = '/accounts/payment-vouchers'
     *   6. Payment-voucher page: on load → reads sessionStorage.pvPrefill → pvOpenCreate(data)
     *   7. User picks bank/cash account → Save (DRAFT) → Post
     *   8. VoucherServiceImpl.post() → processAllocations() → updates JEM.allocatedAmount
     *   9. PurchaseService.confirmInvoice() already set doc.dueAmount = totalAmount
     *      After payment posted, VoucherServiceImpl adjusts supplier balance downward
     */
    @GetMapping("/purchase/docs/payment-prefill")
    @ResponseBody
    public Map<String, Object> paymentPrefill(@RequestParam Long invoiceId) {
        Map<String, Object> res = new HashMap<>();
        try {
            VoucherDTO dto = purchaseService.populatePaymentFromInvoice(invoiceId);
            res.put("success",    true);
            res.put("redirectTo", "/accounts/payment-vouchers");
            res.put("obj",        Map.of("defaultData", dto));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AJAX LOOKUPS ──────────────────────────────────────────────────────────

    @GetMapping("/purchase/docs/open-pos")
    @ResponseBody
    public List<Map<String, Object>> openPOs(@RequestParam Long supplierId) {
        return purchaseService.openPOsForSupplier(supplierId);
    }

    @GetMapping("/purchase/docs/confirmed-grns")
    @ResponseBody
    public List<Map<String, Object>> confirmedGRNs(@RequestParam Long supplierId) {
        return purchaseService.confirmedGRNsForSupplier(supplierId);
    }
}
