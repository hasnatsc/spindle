// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E13  Budget                                               ║
// ║  Tables: bgt_fiscal_years, bgt_budget_heads, bgt_budgets,               ║
// ║           bgt_budget_lines, bgt_budget_revisions,                       ║
// ║           bgt_budget_revision_lines, bgt_actuals,                       ║
// ║           bgt_encumbrances, bgt_transfers, bgt_alerts,                  ║
// ║           bgt_approval_policies, bgt_budget_notes                       ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: budget/entity/FiscalYear.java ──────────────────────────────────────
package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_fiscal_years",
    uniqueConstraints = @UniqueConstraint(name = "uq_bfy_org_code",
        columnNames = {"organization_id", "year_code"}),
    indexes = {
        @Index(name = "idx_bfy_org",     columnList = "organization_id"),
        @Index(name = "idx_bfy_current", columnList = "is_current"),
        @Index(name = "idx_bfy_status",  columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiscalYear extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)  private String yearCode;
    @Column(nullable = false, length = 100) private String yearName;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FiscalYearStatus status = FiscalYearStatus.DRAFT;

    @Builder.Default @Column(nullable = false) private boolean isCurrent = false;

    @Column(length = 100) private String closedBy;
    private LocalDateTime closedAt;
    @Column(columnDefinition = "text") private String notes;

    public enum FiscalYearStatus { DRAFT, ACTIVE, LOCKED, CLOSED }
}