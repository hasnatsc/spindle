package com.asg.spindleserp.budget;

import com.asg.spindleserp.approval.ApprovalConfig;
import com.asg.spindleserp.common.BaseOrgEntity;
import lombok.Getter;

@Entity
@Table(name = "bgt_approval_policies",
        indexes = @Index(name = "idx_bgt_pol_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetApprovalPolicy extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_config_id", nullable = false)
    private ApprovalConfig approvalConfig;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;
    @Column(name = "budget_type", length = 30)
    private String budgetType; // NULL = applies to all

    @Builder.Default
    @Column(name = "min_amount", precision = 18, scale = 2)
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Column(name = "max_amount", precision = 18, scale = 2)
    private BigDecimal maxAmount; // NULL = no upper limit

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ── Helper ────────────────────────────────────────────────────
    public boolean appliesTo(String type, BigDecimal amount) {
        if (budgetType != null && !budgetType.equals(type)) return false;
        if (amount.compareTo(minAmount) < 0) return false;
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) return false;
        return true;
    }
}
