package com.asg.spindleserp.accounts.listener;

import com.asg.spindleserp.approval.event.ApprovalCompletedEvent;
import com.asg.spindleserp.accounts.service.VoucherServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Listens for approval lifecycle events and drives the voucher lifecycle.
 *
 * When an approval request for a voucher is:
 *   APPROVED → posts the voucher (GL entries, balance update, numbering)
 *   REJECTED → marks voucher as REJECTED
 *   RETURNED → reverts voucher to DRAFT so the user can edit & re-submit
 *
 * Document types handled: JOURNAL_VOUCHER, PAYMENT_VOUCHER,
 *                          RECEIPT_VOUCHER, CONTRA_VOUCHER
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherApprovalListener {

    private static final Set<String> VOUCHER_TYPES = Set.of(
        "JOURNAL_VOUCHER", "PAYMENT_VOUCHER", "RECEIPT_VOUCHER", "CONTRA_VOUCHER", "SALES_VOUCHER");

    private final VoucherServiceImpl voucherService;

    @EventListener
    public void handleApprovalCompleted(ApprovalCompletedEvent event) {
        if (!VOUCHER_TYPES.contains(event.getDocumentType())) return;

        log.info("Approval {} for {} #{} — executing voucher action",
            event.getAction(), event.getDocumentType(), event.getReferenceId());

        switch (event.getAction()) {
            case "APPROVED" -> voucherService.completeApproval(event.getReferenceId());
            case "REJECTED" -> voucherService.rejectApproval(event.getReferenceId(), event.getRemarks());
            case "RETURNED" -> voucherService.returnApproval(event.getReferenceId(), event.getRemarks());
            default -> log.warn("Unknown approval action: {}", event.getAction());
        }
    }
}
