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

    void delete(Long id);

    /** DRAFT → CONFIRMED. Creates the GL voucher and updates customer AR. */
    TrvBookingDTO confirm(Long id);

    /** DRAFT → CANCELLED. Confirmed bookings cannot be cancelled here (need reversal flow). */
    TrvBookingDTO cancel(Long id);

    /**
     * Pre-fills a Receipt Voucher for a CONFIRMED booking with due balance.
     * Mirror of SalesServiceImpl.populateReceiptFromInvoice().
     */
    VoucherDTO populateReceiptFromBooking(Long bookingId);

    DataTableResponse datatableList(int draw, int start, int length, String search, String status);

    Map<String, Object> dashboardSummary();

    TrvBookingDTO toDTO(TrvBooking entity);
}
