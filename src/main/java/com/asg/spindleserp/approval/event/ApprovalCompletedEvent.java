package com.asg.spindleserp.approval.event;

import lombok.Getter;

/**
 * Fired when an approval request reaches a terminal action that affects the
 * underlying document — APPROVED, REJECTED, or RETURNED.
 *
 * Listeners (e.g. VoucherApprovalListener) check documentType and react:
 *   APPROVED → post / activate the document
 *   REJECTED → mark as rejected
 *   RETURNED → return to draft
 */
@Getter
public class ApprovalCompletedEvent {

    private final Long   requestId;
    private final Long   referenceId;
    private final String documentType;
    private final String action;       // APPROVED | REJECTED | RETURNED
    private final String remarks;

    public ApprovalCompletedEvent(Long requestId, Long referenceId,
                                  String documentType, String action,
                                  String remarks) {
        this.requestId    = requestId;
        this.referenceId  = referenceId;
        this.documentType = documentType;
        this.action       = action;
        this.remarks      = remarks;
    }
}
