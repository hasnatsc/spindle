package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("GENERAL")
public class GeneralSubAccount extends ChartOfAccountSub {
    @Builder
    public GeneralSubAccount() {
        super();
    }
}
