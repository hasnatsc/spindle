package com.asg.spindleserp.budget;

import com.asg.spindleserp.common.BaseOrgEntity;
import lombok.Getter;

@Entity
@Table(name = "bgt_fiscal_years",
        uniqueConstraints = @UniqueConstraint(name = "uk_fy_org_code",
                columnNames = {"organization_id", "year_code"}),
        indexes = {
                @Index(name = "idx_bgt_fy_org", columnList = "organization_id"),
                @Index(name = "idx_bgt_fy_current", columnList = "organization_id,is_current")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetFiscalYear extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_code", nullable = false, length = 20)
    private String yearCode;   // FY2025-26
    @Column(name = "year_name", nullable = false, length = 100)
    private String yearName;   // Financial Year 2025-2026
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";
    // DRAFT | ACTIVE | LOCKED | CLOSED

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @Column(name = "closed_by", length = 100)
    private String closedBy;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Builder.Default
    @OneToMany(mappedBy = "fiscalYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Budget> budgets = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isOpen() {
        return "ACTIVE".equals(status);
    }

    public boolean isLocked() {
        return "LOCKED".equals(status) || "CLOSED".equals(status);
    }

    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
