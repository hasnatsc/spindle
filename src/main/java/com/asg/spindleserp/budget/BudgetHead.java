package com.asg.spindleserp.budget;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bgt_budget_heads",
        uniqueConstraints = @UniqueConstraint(name = "uk_bgt_head_org_code",
                columnNames = {"organization_id", "head_code"}),
        indexes = {
                @Index(name = "idx_bgt_head_org", columnList = "organization_id"),
                @Index(name = "idx_bgt_head_parent", columnList = "parent_id"),
                @Index(name = "idx_bgt_head_type", columnList = "head_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetHead extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BudgetHead parent;

    @Column(name = "head_code", nullable = false, length = 50)
    private String headCode;
    @Column(name = "head_name", nullable = false, length = 200)
    private String headName;

    @Enumerated(EnumType.STRING)
    @Column(name = "head_type", nullable = false, length = 30)
    @Builder.Default
    private BudgetHeadType headType = BudgetHeadType.EXPENSE;

    @Column(columnDefinition = "TEXT")
    private String description;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetHead> children = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "budgetHead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetLine> budgetLines = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    public boolean isRevenue() {
        return BudgetHeadType.REVENUE.equals(headType);
    }

    public boolean isCapex() {
        return BudgetHeadType.CAPEX.equals(headType);
    }

    public String getFullPath() {
        if (parent != null) return parent.getFullPath() + " > " + headName;
        return headName;
    }
}
