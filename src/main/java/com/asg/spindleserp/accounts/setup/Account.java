package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "acc_chart_of_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_coa_org_code", columnNames = {"organization_id", "account_code"}),
        indexes = {
                @Index(name = "idx_coa_org", columnList = "organization_id"),
                @Index(name = "idx_coa_type", columnList = "account_type"),
                @Index(name = "idx_coa_parent", columnList = "parent_account_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;     // ASSET|LIABILITY|EQUITY|REVENUE|EXPENSE

    @Column(name = "account_nature", nullable = false, length = 10)
    private String accountNature;   // DEBIT|CREDIT

    @Builder.Default
    @Column(nullable = false)
    private Integer level = 1;
    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";
    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;
    @Builder.Default
    @Column(name = "is_control_account", nullable = false)
    private Boolean isControlAccount = false;
    @Builder.Default
    @Column(name = "allow_manual_entry", nullable = false)
    private Boolean allowManualEntry = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
