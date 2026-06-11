package com.asg.spindleserp.hrm.payroll;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hrm_payroll_runs",
        uniqueConstraints = @UniqueConstraint(name = "uk_payroll_org_month", columnNames = {"organization_id", "payroll_month"}),
        indexes = @Index(name = "idx_payroll_month", columnList = "organization_id,payroll_month"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_month", nullable = false, length = 7)
    private String payrollMonth; // YYYY-MM
    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Builder.Default
    @Column(name = "total_gross", precision = 18, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "total_deductions", precision = 18, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "total_net", precision = 18, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "employee_count")
    private Integer employeeCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayrollRunLine> lines = new ArrayList<>();
}
