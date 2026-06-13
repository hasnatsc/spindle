package com.asg.spindleserp.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "hrm_attendances",
        uniqueConstraints = @UniqueConstraint(name = "uq_att_emp_date",
                columnNames = {"employee_id", "att_date"}),
        indexes = {
                @Index(name = "idx_att_emp", columnList = "employee_id"),
                @Index(name = "idx_att_date", columnList = "att_date"),
                @Index(name = "idx_att_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate attDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    @Column(precision = 5, scale = 2)
    private BigDecimal workingHours;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Attendance.AttendanceStatus status = Attendance.AttendanceStatus.ABSENT;

    @Builder.Default
    @Column(length = 20)
    private String source = "MANUAL";
    @Column(length = 500)
    private String remarks;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AttendanceStatus {PRESENT, ABSENT, LATE, HALF_DAY, HOLIDAY, LEAVE, WEEKEND}
}
