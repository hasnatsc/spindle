package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bgt_budget_heads",
        uniqueConstraints = @UniqueConstraint(name = "uq_bbh_org_code",
                columnNames = {"organization_id", "head_code"}),
        indexes = {
                @Index(name = "idx_bbh_org", columnList = "organization_id"),
                @Index(name = "idx_bbh_parent", columnList = "parent_id"),
                @Index(name = "idx_bbh_type", columnList = "head_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetHead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BudgetHead parent;

    @Column(nullable = false, length = 50)
    private String headCode;
    @Column(nullable = false, length = 200)
    private String headName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetHead.HeadType headType = BudgetHead.HeadType.EXPENSE;

    @Column(columnDefinition = "text")
    private String description;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private int displayOrder = 0;

    public enum HeadType {REVENUE, EXPENSE, CAPEX, OPEX, PRODUCTION, HR, COMMERCIAL, OTHER}
}
