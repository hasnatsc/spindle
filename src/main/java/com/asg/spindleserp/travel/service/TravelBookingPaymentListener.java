package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.accounts.event.VoucherPostedEvent;
import com.asg.spindleserp.accounts.repository.JournalEntryMasterRepository;
import com.asg.spindleserp.accounts.service.VoucherService;
import com.asg.spindleserp.travel.entity.TrvBooking;
import com.asg.spindleserp.travel.repository.TrvBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Listens for voucher-posting events from the Accounts module and keeps
 * travel-booking payment status in sync.
 * <p>
 * When a RECEIPT_VOUCHER referencing a booking is posted (or reversed):
 * 1. Auto-allocates against the SALES_VOUCHER (if not already allocated)
 * 2. Recalculates booking paidAmount / dueAmount / status
 * <p>
 * Both direct posts (no approval) and posts after approval are handled identically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelBookingPaymentListener {

    private final TrvBookingRepository bookingRepo;
    private final JournalEntryMasterRepository jemRepo;
    private final VoucherService voucherService;
    private final JdbcTemplate jdbcTemplate;

    @EventListener
    @Transactional
    public void handleVoucherPosted(VoucherPostedEvent event) {
        if (event.getOrganizationId() == null) return;

        String vType = event.getVoucherType();
        String vStatus = event.getVoucherStatus();

        if ("RECEIPT_VOUCHER".equals(vType) && "POSTED".equals(vStatus)) {
            if (event.isReversal()) {
                handleReceiptVoucherReversal(event);
            } else {
                handleReceiptVoucher(event);
            }
        }
    }

    /**
     * Handles a direct RECEIPT_VOUCHER posting: referenceNo = bookingNo.
     */
    private void handleReceiptVoucher(VoucherPostedEvent event) {
        // 1. Auto-allocate against the SALES_VOUCHER
        processAutoAllocation(event.getVoucherId(), event.getOrganizationId());

        // 2. Update booking payment status (if referenceNo matches a booking)
        if (event.getReferenceNo() == null) return;
        bookingRepo.findByOrganizationIdAndBookingNo(
                event.getOrganizationId(), event.getReferenceNo()
        ).ifPresent(booking -> {
            BigDecimal totalReceived = sumReceiptsForBooking(
                    event.getOrganizationId(), event.getReferenceNo());
            updateBookingPaymentStatus(booking, totalReceived);
            log.info("Booking {} payment synced via RECEIPT_VOUCHER #{}: paid={}, status={}",
                    booking.getBookingNo(), event.getVoucherId(),
                    totalReceived, booking.getStatus());
        });
    }

    /**
     * Auto-allocates the posted RECEIPT_VOUCHER against its SALES_VOUCHER.
     * The SALES_VOUCHER ID is embedded in the RECEIPT_VOUCHER narration
     * as {@code [SALES_VOUCHER:123]} when created via booking confirm.
     * Skips if allocations already exist (e.g., via Accounts UI manual allocation).
     */
    private void processAutoAllocation(Long receiptVoucherId, Long orgId) {
        try {
            String checkSql = "SELECT COUNT(*) FROM acc_voucher_allocations WHERE paying_voucher_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, receiptVoucherId);
            if (count != null && count > 0) return; // already allocated

            jemRepo.findById(receiptVoucherId).ifPresent(rv -> {
                String narration = rv.getNarration();
                if (narration == null) return;

                // Parse [SALES_VOUCHER:123] from narration
                int start = narration.indexOf("[SALES_VOUCHER:");
                if (start < 0) return;
                int end = narration.indexOf("]", start);
                if (end < 0) return;
                Long salesVoucherId = Long.parseLong(narration.substring(start + 15, end));

                jemRepo.findById(salesVoucherId).ifPresent(salesJem -> {
                    VoucherDTO.AllocationDTO alloc = new VoucherDTO.AllocationDTO();
                    alloc.setSourceVoucherId(salesVoucherId);
                    alloc.setAllocatedAmount(rv.getTotalAmount());
                    alloc.setAllocationDate(LocalDate.now());
                    alloc.setNarration("Auto-allocation from booking confirm");
                    voucherService.processAllocations(rv, List.of(alloc));
                    log.info("Auto-allocated RECEIPT_VOUCHER #{} ({}) against SALES_VOUCHER #{} ({})",
                            rv.getId(), rv.getVoucherNo(), salesVoucherId, salesJem.getVoucherNo());
                });
            });
        } catch (Exception e) {
            log.warn("Could not auto-allocate RECEIPT_VOUCHER #{}: {}", receiptVoucherId, e.getMessage());
        }
    }

    /** Shared logic: update booking paidAmount/dueAmount/status from total collected. */
    private void updateBookingPaymentStatus(TrvBooking booking, BigDecimal totalReceived) {
        if (booking.getStatus() != TrvBooking.Status.CONFIRMED
                && booking.getStatus() != TrvBooking.Status.PARTIALLY_PAID
                && booking.getStatus() != TrvBooking.Status.PAID) {
            return;
        }
        booking.setPaidAmount(totalReceived);
        booking.setDueAmount(booking.getTotalAmount().subtract(totalReceived));

        if (totalReceived.compareTo(BigDecimal.ZERO) > 0
                && totalReceived.compareTo(booking.getTotalAmount()) >= 0) {
            booking.setStatus(TrvBooking.Status.PAID);
        } else if (totalReceived.compareTo(BigDecimal.ZERO) > 0) {
            booking.setStatus(TrvBooking.Status.PARTIALLY_PAID);
        } else {
            booking.setStatus(TrvBooking.Status.CONFIRMED);
        }

        booking.setUpdatedBy("system");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepo.save(booking);
    }

    /**
     * Handles a RECEIPT_VOUCHER reversal: the reversal voucher's reversedVoucherId
     * points to the original RECEIPT_VOUCHER. We find the original's referenceNo
     * (= bookingNo) and recalculate the booking's payment status.
     */
    private void handleReceiptVoucherReversal(VoucherPostedEvent event) {
        try {
            jemRepo.findById(event.getReversedVoucherId()).ifPresent(original -> {
                String bookingNo = original.getReferenceNo();
                if (bookingNo == null) return;

                bookingRepo.findByOrganizationIdAndBookingNo(
                        event.getOrganizationId(), bookingNo
                ).ifPresent(booking -> {
                    BigDecimal totalReceived = sumReceiptsForBooking(
                            event.getOrganizationId(), bookingNo);
                    updateBookingPaymentStatus(booking, totalReceived);
                    log.info("Booking {} payment synced after RECEIPT_VOUCHER reversal #{}: paid={}, status={}",
                            booking.getBookingNo(), event.getVoucherId(),
                            totalReceived, booking.getStatus());
                });
            });
        } catch (Exception e) {
            log.warn("Could not process RECEIPT_VOUCHER reversal for event {}: {}",
                    event.getVoucherId(), e.getMessage());
        }
    }

    /**
     * Sums all posted (non-reversed) RECEIPT_VOUCHER amounts linked to a booking
     * via referenceNo = bookingNo. This naturally handles partial collections
     * and corrections — the sum always reflects reality.
     */
    private BigDecimal sumReceiptsForBooking(Long orgId, String bookingNo) {
        String sql = """
            SELECT COALESCE(SUM(j.total_amount), 0)
            FROM acc_journal_entry_master j
            WHERE j.organization_id = ?
              AND j.voucher_type    = 'RECEIPT_VOUCHER'
              AND j.voucher_status  = 'POSTED'
              AND j.is_reversed     = FALSE
              AND j.reference_no    = ?
            """;
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, orgId, bookingNo);
        return result != null ? result : BigDecimal.ZERO;
    }
}
