package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hrm_payroll_runs",
        uniqueConstraints = @UniqueConstraint(name = "uq_pr_org_month",
                columnNames = {"organization_id", "payroll_month"}),
        indexes = {
                @Index(name = "idx_pr_org", columnList = "organization_id"),
                @Index(name = "idx_pr_month", columnList = "payroll_month"),
                @Index(name = "idx_pr_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @Column(nullable = false, length = 7)
    private String payrollMonth;  // YYYY-MM
    @Column(nullable = false)
    private LocalDate runDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollRun.PayrollStatus status = PayrollRun.PayrollStatus.DRAFT;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false)
    private int employeeCount = 0;

    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(columnDefinition = "text")
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollRunLine> lines = new ArrayList<>();

    public enum PayrollStatus {DRAFT, PROCESSING, COMPLETED, APPROVED, PAID, CANCELLED}
}
