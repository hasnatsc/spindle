package com.asg.spindleserp.purchase.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.purchase.dto.PurchaseDocumentDTO;

import java.util.List;
import java.util.Map;

/**
 * PurchaseService
 *
 * Manages the full purchase cycle:
 *   PURCHASE_ORDER → GOODS_RECEIPT_NOTE → PURCHASE_INVOICE → PAYMENT_VOUCHER
 *                                                          → DEBIT_NOTE (optional return)
 *
 * Key additions vs original:
 *   • confirmInvoice() now creates a PURCHASE_VOUCHER JournalEntryMaster
 *     so that Payment Vouchers can allocate against it.
 *   • populatePaymentFromInvoice() returns a pre-filled VoucherDTO for the
 *     Payment Voucher form — called by createPaymentFromInvoice(piId) in JS.
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
     *  - PI    → creates PURCHASE_VOUCHER JEM (DR purchases / CR AP),
     *            updates supplier currentBalance, sets accounting_posted = true
     *  - DN    → posts SUPPLIER_RETURN inventory transactions (outbound), reduces AP
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
     * @param parentId  the confirmed source document
     * @param childType the target document type (GOODS_RECEIPT_NOTE, PURCHASE_INVOICE, DEBIT_NOTE)
     */
    PurchaseDocumentDTO populateFromSource(Long parentId, String childType);

    /**
     * NEW: Returns a pre-filled VoucherDTO for the Payment Voucher form.
     *
     * Called when the user clicks "Make Payment" (💰) on a confirmed Purchase Invoice row.
     * The returned DTO contains:
     *   - voucherType = PAYMENT_VOUCHER
     *   - partyId / partyType = supplier
     *   - totalAmount = invoice dueAmount
     *   - allocations[0] = full settlement against the invoice's JournalEntryMaster
     *
     * The frontend opens /accounts/payment-vouchers, pre-fills the form from this DTO,
     * user selects the bank/cash account, then saves + posts the voucher.
     *
     * @param invoiceId the confirmed PURCHASE_INVOICE BusinessDocument id
     */
    VoucherDTO populatePaymentFromInvoice(Long invoiceId);

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
