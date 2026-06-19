package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

// ── STI subclasses ─────────────────────────────────────────────────────────
@Entity
@DiscriminatorValue("BANK")
public class BankAccount extends ChartOfAccountSub {
    @Builder
    public BankAccount() {
        super();
    }
}
