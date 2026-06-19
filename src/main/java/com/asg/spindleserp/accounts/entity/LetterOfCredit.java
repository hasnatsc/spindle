package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

@Entity
@DiscriminatorValue("LC")
public class LetterOfCredit extends ChartOfAccountSub {
    @Builder
    public LetterOfCredit() {
        super();
    }
}
