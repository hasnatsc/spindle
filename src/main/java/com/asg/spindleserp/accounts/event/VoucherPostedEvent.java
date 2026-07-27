package com.asg.spindleserp.accounts.event;

import lombok.Getter;

/**
 * Fired after a voucher reaches POSTED status (either directly or via approval completion).
 * Downstream modules (Travel, Purchase, etc.) can listen for this event to sync
 * their own payment-status tracking when a RECEIPT_VOUCHER or PAYMENT_VOUCHER
 * referencing their documents is posted.
 */
@Getter
public class VoucherPostedEvent {

    private final Long   voucherId;
    private final String voucherType;
    private final String voucherStatus;
    private final String referenceNo;
    private final Long   organizationId;
    private final Long   reversedVoucherId;

    public VoucherPostedEvent(Long voucherId, String voucherType, String voucherStatus,
                              String referenceNo, Long organizationId) {
        this(voucherId, voucherType, voucherStatus, referenceNo, organizationId, null);
    }

    public VoucherPostedEvent(Long voucherId, String voucherType, String voucherStatus,
                              String referenceNo, Long organizationId, Long reversedVoucherId) {
        this.voucherId = voucherId;
        this.voucherType = voucherType;
        this.voucherStatus = voucherStatus;
        this.referenceNo = referenceNo;
        this.organizationId = organizationId;
        this.reversedVoucherId = reversedVoucherId;
    }

    public boolean isReversal() {
        return reversedVoucherId != null;
    }
}
