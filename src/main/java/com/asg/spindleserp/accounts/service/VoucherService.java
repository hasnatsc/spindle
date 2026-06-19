package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

/**
 * VoucherService
 *
 * Handles all four voucher types via the same interface.
 * voucherType discriminates business logic in the implementation.
 */
public interface VoucherService {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    VoucherDTO save(VoucherDTO dto);

    VoucherDTO findById(Long id);

    void delete(Long id);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Post voucher: validates balance, writes GL, updates sub-account balance, allocates */
    VoucherDTO post(Long id);

    /** Reverse a posted voucher: creates mirror entry, marks original as REVERSED */
    VoucherDTO reverse(Long id, String narration);

    // ── DataTable listing per type ────────────────────────────────────────────

    DataTableResponse datatableList(String voucherType, int draw, int start, int length, String search);

    // ── Allocation helpers ────────────────────────────────────────────────────

    /**
     * Returns open (unsettled) vouchers for a party — for the allocation picker.
     * [{id, voucherNo, voucherType, voucherDate, dueDate, totalAmount, dueAmount}]
     */
    List<Map<String, Object>> openVouchersForParty(Long partyId, String partyType);

    // ── Aging ─────────────────────────────────────────────────────────────────

    /**
     * Aging summary for AP (partyType=SUPPLIER) or AR (partyType=CUSTOMER).
     * Buckets: current / 0-30 / 31-60 / 61-90 / 90+
     * Each row: {partyId, partyCode, partyName, current, b30, b60, b90, b90plus, total}
     */
    DataTableResponse agingSummary(String partyType, int draw, int start, int length, String search);

    /**
     * Aging detail for a single party — lists each open invoice with days overdue.
     */
    List<Map<String, Object>> agingDetail(Long partyId, String partyType);

    // ── Mapping helper ────────────────────────────────────────────────────────

    VoucherDTO toDTO(JournalEntryMaster entity);
}
