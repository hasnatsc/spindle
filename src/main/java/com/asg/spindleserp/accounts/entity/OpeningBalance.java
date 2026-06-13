package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "acc_opening_balances",
        indexes = @Index(name = "idx_ob_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpeningBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accounting_period_id", nullable = false)
    private AccountingPeriod accountingPeriod;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingDebitBalance = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCreditBalance = BigDecimal.ZERO;

    @Column(length = 50)
    private String balanceType;
    private Boolean isActive;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPosted = false;
    @Column(length = 100)
    private String postedBy;
    private LocalDate postedDate;
    @Column(length = 1000)
    private String remarks;
}
