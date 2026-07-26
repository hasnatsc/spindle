package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

/** bKash / Nagad / Rocket / Upay sub-ledger. */
@Entity
@DiscriminatorValue("MOBILE_BANKING")
public class MobileBankingAccount extends ChartOfAccountSub {
    @Builder
    public MobileBankingAccount() {
        super();
    }
}
