package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

/** Closed-loop wallet / store credit / gift card sub-ledger. */
@Entity
@DiscriminatorValue("WALLET")
public class WalletAccount extends ChartOfAccountSub {
    @Builder
    public WalletAccount() {
        super();
    }
}
