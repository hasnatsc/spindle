package com.asg.spindleserp.accounts.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;

/** POS acquiring / card receivable sub-ledger (Visa POS, MasterCard POS). */
@Entity
@DiscriminatorValue("CARD")
public class CardAccount extends ChartOfAccountSub {
    @Builder
    public CardAccount() {
        super();
    }
}
