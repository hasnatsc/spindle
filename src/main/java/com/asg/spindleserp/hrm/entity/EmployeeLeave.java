package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.approval.entity.ApprovalRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_leaves",
        indexes = {
                @Index(name = "idx_leave_emp", columnList = "employee_id"),
                @Index(name = "idx_leave_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeave extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(nullable = false, length = 30)
    private String leaveType;
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal totalDays;
    @Column(columnDefinition = "text")
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeLeave.LeaveStatus status = EmployeeLeave.LeaveStatus.PENDING;

    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;

    public enum LeaveStatus {PENDING, APPROVED, REJECTED, CANCELLED}
}
