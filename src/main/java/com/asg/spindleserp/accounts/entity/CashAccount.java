package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("CASH")
class CashAccount extends ChartOfAccountSub {
    @Builder
    public CashAccount() {
        super();
    }
}
