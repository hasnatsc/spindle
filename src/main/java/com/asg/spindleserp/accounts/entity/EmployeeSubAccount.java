package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("EMPLOYEE")
public class EmployeeSubAccount extends ChartOfAccountSub {
    @Builder
    public EmployeeSubAccount() {
        super();
    }
}
