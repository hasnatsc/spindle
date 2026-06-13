package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

// ── STI subclasses ─────────────────────────────────────────────────────────
@Entity
@DiscriminatorValue("BANK")
class BankAccount extends ChartOfAccountSub {
    @Builder
    public BankAccount() {
        super();
    }
}
