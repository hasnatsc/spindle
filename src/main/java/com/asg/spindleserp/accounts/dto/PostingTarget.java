package com.asg.spindleserp.accounts.dto;

import com.asg.spindleserp.common.enums.PaymentMode;

/**
 * Immutable result of resolving a payment into GL coordinates.
 * <p>
 * Every module that moves money calls PaymentAccountResolver and receives one
 * of these; nothing downstream needs to know whether the money came from a cash
 * box, DBBL, bKash Merchant or a Visa POS terminal.
 *
 * @param paymentAccountId acc_payment_accounts.id actually used
 * @param ledgerAccountId  acc_chart_of_accounts.id — the DR/CR head
 * @param subAccountId     acc_chart_of_accounts_sub.id — nullable
 * @param chargeAccountId  acc_chart_of_accounts.id for MDR / cash-out charges — nullable
 * @param paymentMode      mode that produced this target
 * @param displayName      label for narration templates, e.g. "bKash Merchant"
 */
public record PostingTarget(
        Long paymentAccountId,
        Long ledgerAccountId,
        Long subAccountId,
        Long chargeAccountId,
        PaymentMode paymentMode,
        String displayName
) {
    public boolean hasSubLedger() {
        return subAccountId != null;
    }

    public boolean hasChargeAccount() {
        return chargeAccountId != null;
    }
}
