package com.asg.spindleserp.hrm.attendance;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_leaves",
        indexes = {
                @Index(name = "idx_leave_emp", columnList = "employee_id"),
                @Index(name = "idx_leave_status", columnList = "status"),
                @Index(name = "idx_leave_dates", columnList = "start_date,end_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeave extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "leave_type", nullable = false, length = 30)
    private String leaveType; // ANNUAL|SICK|CASUAL|MATERNITY|PATERNITY|UNPAID|COMPENSATORY

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "total_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalDays;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    // PENDING|APPROVED|REJECTED|CANCELLED
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
