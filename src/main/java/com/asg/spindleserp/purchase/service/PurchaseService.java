package com.asg.spindleserp.purchase.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.purchase.dto.PurchaseDocumentDTO;

import java.util.List;
import java.util.Map;

/**
 * PurchaseService
 *
 * Manages the full purchase cycle:
 *   PURCHASE_ORDER → GOODS_RECEIPT_NOTE → PURCHASE_INVOICE → DEBIT_NOTE
 *
 * Each document type shares BusinessDocument entity but has distinct
 * status lifecycle, stock-posting rules, and DataTable JS function prefixes.
 *
 * Key feature: populateFromSource(parentId) copies lines from a confirmed
 * parent document into a new draft child — enabling the "create GRN from PO"
 * workflow directly from the DataTable action buttons.
 */
public interface PurchaseService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    PurchaseDocumentDTO save(PurchaseDocumentDTO dto);

    PurchaseDocumentDTO findById(Long id);

    void delete(Long id);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Confirms the document:
     *  - PO    → status = CONFIRMED (no stock movement)
     *  - GRN   → posts PURCHASE_RECEIPT inventory transactions, creates lots if needed
     *  - PI    → posts payable amount to ChartOfAccountSub (partyId)
     *  - DN    → posts SUPPLIER_RETURN inventory transactions (outbound)
     */
    PurchaseDocumentDTO confirm(Long id);

    /** Cancels a DRAFT document */
    PurchaseDocumentDTO cancel(Long id);

    // ── Cascade populate ──────────────────────────────────────────────────────

    /**
     * Returns a pre-populated PurchaseDocumentDTO ready for the child form.
     * All lines are copied from the source document; sourceLineId is set for traceability.
     *
     * PO → GRN:  copies quantity → receivedQty; unitPrice preserved
     * GRN → PI:  copies receivedQty → quantity; unitPrice / lineAmount preserved
     * PI → DN:   copies quantity; reason type set
     *
     * @param parentId     the confirmed source document
     * @param childType    the target document type (GOODS_RECEIPT_NOTE, PURCHASE_INVOICE, DEBIT_NOTE)
     */
    PurchaseDocumentDTO populateFromSource(Long parentId, String childType);

    // ── DataTable listing ─────────────────────────────────────────────────────

    DataTableResponse datatableList(String documentType, int draw, int start, int length, String search);

    // ── AJAX helpers ──────────────────────────────────────────────────────────

    /** Returns confirmed POs for a supplier (used in GRN "select PO" picker) */
    List<Map<String, Object>> openPOsForSupplier(Long supplierId);

    /** Returns confirmed GRNs for a supplier (used in Invoice "select GRN" picker) */
    List<Map<String, Object>> confirmedGRNsForSupplier(Long supplierId);

    // ── Mapping helper ────────────────────────────────────────────────────────

    PurchaseDocumentDTO toDTO(BusinessDocument entity);
}
