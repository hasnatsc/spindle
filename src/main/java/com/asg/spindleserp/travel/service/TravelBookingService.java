package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.travel.dto.TrvBookingDTO;
import com.asg.spindleserp.travel.entity.TrvBooking;

import java.util.Map;

/**
 * TravelBookingService — booking header + services + passengers cycle.
 *
 * DRAFT → CONFIRMED → (PARTIALLY_PAID → PAID via Receipt Voucher allocation) → COMPLETED
 *                   → CANCELLED
 *
 * Mirror of SalesService's Sales Invoice handling: confirm() creates a
 * SALES_VOUCHER JournalEntryMaster (DR Accounts Receivable / CR Travel
 * Revenue) so Receipt Vouchers can allocate against it exactly the same
 * way they allocate against a Sales Invoice — no new voucher type needed.
 */
public interface TravelBookingService {

    TrvBookingDTO save(TrvBookingDTO dto);

    TrvBookingDTO findById(Long id);

    TrvBookingDTO findByBookingNo(Long organizationId, String bookingNo);

    void delete(Long id);

    /** DRAFT → CONFIRMED. Creates the GL voucher and updates customer AR. */
    TrvBookingDTO confirm(Long id);

    /** DRAFT → CANCELLED. */
    TrvBookingDTO cancel(Long id);

    /**
     * CONFIRMED/PARTIALLY_PAID/PAID → CANCELLED with GL reversal.
     * Reverses the SALES_VOUCHER and any RECEIPT_VOUCHERs, then marks the booking CANCELLED.
     */
    TrvBookingDTO reverse(Long id, String reason);

    /**
     * Reverses specific RECEIPT_VOUCHERs for a booking (partial refund) without
     * cancelling the entire booking. After reversal the booking's paidAmount,
     * dueAmount, and status are recalculated.
     *
     * @param id                booking ID
     * @param receiptVoucherIds RECEIPT_VOUCHER IDs to reverse
     * @param reason            optional reason for the refund
     * @return updated booking DTO
     */
    TrvBookingDTO partialRefund(Long id, java.util.List<Long> receiptVoucherIds, String reason);

    /**
     * Pre-fills a Receipt Voucher for a CONFIRMED booking with due balance.
     * Mirror of SalesServiceImpl.populateReceiptFromInvoice().
     */
    VoucherDTO populateReceiptFromBooking(Long bookingId);

    DataTableResponse datatableList(int draw, int start, int length, String search, String status);

    Map<String, Object> dashboardSummary();

    TrvBookingDTO toDTO(TrvBooking entity);
}
