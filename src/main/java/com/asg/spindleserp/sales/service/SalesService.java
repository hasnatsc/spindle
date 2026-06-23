package com.asg.spindleserp.sales.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.sales.dto.SalesDocumentDTO;

import java.util.List;
import java.util.Map;

/**
 * SalesService — full sales cycle.
 *
 *   SALES_ORDER → DELIVERY_ORDER → SALES_INVOICE → RECEIPT_VOUCHER
 *                                                → CREDIT_NOTE (optional)
 *
 * Key addition vs original:
 *   confirmInvoice() creates a SALES_VOUCHER JournalEntryMaster
 *   so Receipt Vouchers can allocate against it.
 *
 *   populateReceiptFromInvoice() returns pre-filled VoucherDTO for the
 *   Receipt Voucher form — mirror of PurchaseService.populatePaymentFromInvoice().
 */
public interface SalesService {

    SalesDocumentDTO save(SalesDocumentDTO dto);

    SalesDocumentDTO findById(Long id);

    void delete(Long id);

    /**
     * Confirms the document:
     *  - SO       → status = CONFIRMED (no stock movement)
     *  - Delivery → posts SALES_ISSUE inventory transactions (stock OUT)
     *  - Invoice  → creates SALES_VOUCHER JEM (DR AR / CR Revenue),
     *               updates customer currentBalance, sets accountingPosted = true
     *  - CN       → posts RETURN_FROM_CUSTOMER (stock IN), reduces AR
     */
    SalesDocumentDTO confirm(Long id);

    SalesDocumentDTO cancel(Long id);

    /**
     * Populates a child document from a confirmed parent.
     * SO → Delivery:  qty → deliveredQty
     * Delivery → Invoice: deliveredQty → qty
     * Invoice → Credit Note: qty for return
     */
    SalesDocumentDTO populateFromSource(Long parentId, String childType);

    /**
     * Returns a pre-filled VoucherDTO for the Receipt Voucher form.
     * Called when user clicks "Collect Payment" (💵) on a confirmed SI row.
     *
     * Returns:
     *   voucherType = RECEIPT_VOUCHER, partyType = CUSTOMER
     *   totalAmount = invoice dueAmount
     *   allocations[0] = full settlement against SI SALES_VOUCHER JEM
     *
     * Frontend: sessionStorage.rvPrefill → /accounts/receipt-vouchers
     */
    VoucherDTO populateReceiptFromInvoice(Long invoiceId);

    DataTableResponse datatableList(String documentType, int draw, int start, int length, String search);

    List<Map<String, Object>> openSOsForCustomer(Long customerId);

    List<Map<String, Object>> confirmedDeliveriesForCustomer(Long customerId);

    SalesDocumentDTO toDTO(BusinessDocument entity);
}
