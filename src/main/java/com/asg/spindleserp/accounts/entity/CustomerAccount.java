package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("CUSTOMER")
public class CustomerAccount extends ChartOfAccountSub {
    @Builder
    public CustomerAccount() {
        super();
    }
}
