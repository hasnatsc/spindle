package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_opening_balances",
        uniqueConstraints = @UniqueConstraint(name = "uk_ob_org_acc_yr",
                columnNames = {"organization_id", "account_id", "fiscal_year"}),
        indexes = {
                @Index(name = "idx_ob_org", columnList = "organization_id"),
                @Index(name = "idx_ob_account", columnList = "account_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpeningBalance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;  // e.g. 2025

    @Builder.Default
    @Column(name = "opening_debit_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingDebitBalance = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "opening_credit_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCreditBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_posted", nullable = false)
    private Boolean isPosted = false;
    @Column(name = "balance_type", length = 20)
    private String balanceType;   // ASSET|LIABILITY|...
    @Column(name = "posted_date")
    private LocalDate postedDate;
    @Column(name = "posted_by", length = 100)
    private String postedBy;
    @Column(length = 1000)
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
