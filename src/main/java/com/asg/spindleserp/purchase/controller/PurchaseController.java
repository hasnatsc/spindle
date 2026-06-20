package com.asg.spindleserp.purchase.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.purchase.dto.PurchaseDocumentDTO;
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

/**
 * PurchaseController — one controller, one service, four document pages.
 *
 * Pages:
 *   GET /purchase/orders          → purchase-orders.html
 *   GET /purchase/grns            → purchase-grns.html
 *   GET /purchase/invoices        → purchase-invoices.html
 *   GET /purchase/debit-notes     → purchase-debit-notes.html
 *
 * Shared REST (discriminated by documentType param or body):
 *   GET    /purchase/docs/list?documentType=PURCHASE_ORDER
 *   GET    /purchase/docs/show/{id}
 *   POST   /purchase/docs/save
 *   POST   /purchase/docs/confirm/{id}
 *   POST   /purchase/docs/cancel/{id}
 *   DELETE /purchase/docs/delete/{id}
 *   GET    /purchase/docs/populate?parentId=&childType=   ← cascade populate
 *   GET    /purchase/docs/open-pos?supplierId=             ← PO picker for GRN
 *   GET    /purchase/docs/confirmed-grns?supplierId=       ← GRN picker for Invoice
 *
 * JS prefix: po / grn / pi / dn
 * Cascade functions called from DataTable action buttons:
 *   createGRNFromPO(poId)           → calls /purchase/docs/populate?parentId=&childType=GOODS_RECEIPT_NOTE
 *   createInvoiceFromGRN(grnId)     → calls /purchase/docs/populate?parentId=&childType=PURCHASE_INVOICE
 *   createPaymentFromInvoice(invId) → opens payment voucher modal pre-filled with invoice
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    // ── Pages ──────────────────────────────────────────────────────────────────

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

    // ── DataTable ─────────────────────────────────────────────────────────────

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

    // ── Show ──────────────────────────────────────────────────────────────────

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

    // ── Save (create / update DRAFT) ──────────────────────────────────────────

    @PostMapping("/purchase/docs/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid PurchaseDocumentDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            PurchaseDocumentDTO saved = purchaseService.save(dto);
            res.put("success", true);
            res.put("id",      saved.getId());
            res.put("documentNo", saved.getDocumentNo());
            res.put("message", dto.getId() != null ? "Document updated." : "Document saved as draft.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

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

    // ── Cancel ────────────────────────────────────────────────────────────────

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

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/purchase/docs/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { purchaseService.delete(id); res.put("success", true); res.put("message", "Document deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Cascade populate ──────────────────────────────────────────────────────

    /**
     * Populates a new child DTO from a confirmed parent document.
     * Called by frontend cascade buttons: createGRNFromPO / createInvoiceFromGRN.
     */
    @GetMapping("/purchase/docs/populate")
    @ResponseBody
    public Map<String, Object> populate(@RequestParam Long   parentId,
                                         @RequestParam String childType) {
        Map<String, Object> res = new HashMap<>();
        try {
            PurchaseDocumentDTO dto = purchaseService.populateFromSource(parentId, childType);
            res.put("success", true);
            res.put("obj", Map.of("defaultData", dto));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AJAX lookups ──────────────────────────────────────────────────────────

    /** Confirmed POs for a supplier — displayed in GRN form "select PO" picker */
    @GetMapping("/purchase/docs/open-pos")
    @ResponseBody
    public List<Map<String, Object>> openPOs(@RequestParam Long supplierId) {
        return purchaseService.openPOsForSupplier(supplierId);
    }

    /** Confirmed GRNs for a supplier — displayed in Invoice form "select GRN" picker */
    @GetMapping("/purchase/docs/confirmed-grns")
    @ResponseBody
    public List<Map<String, Object>> confirmedGRNs(@RequestParam Long supplierId) {
        return purchaseService.confirmedGRNsForSupplier(supplierId);
    }
}
