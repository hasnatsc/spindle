package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("INTER_COMPANY")
class InterCompanyAccount extends ChartOfAccountSub {
    @Builder
    public InterCompanyAccount() {
        super();
    }
}
