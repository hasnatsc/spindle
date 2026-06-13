package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("SUPPLIER")
class SupplierAccount extends ChartOfAccountSub {
    @Builder
    public SupplierAccount() { super(); }
}