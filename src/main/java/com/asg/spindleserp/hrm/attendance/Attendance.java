package com.asg.spindleserp.hrm.attendance;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hrm_attendances",
        uniqueConstraints = @UniqueConstraint(name = "uk_att_emp_date", columnNames = {"employee_id", "att_date"}),
        indexes = {
                @Index(name = "idx_att_emp_date", columnList = "employee_id,att_date"),
                @Index(name = "idx_att_org_date", columnList = "organization_id,att_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "att_date", nullable = false)
    private LocalDate attDate;
    @Column(name = "check_in")
    private LocalTime checkIn;
    @Column(name = "check_out")
    private LocalTime checkOut;
    @Column(name = "working_hours", precision = 5, scale = 2)
    private BigDecimal workingHours;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ABSENT"; // PRESENT|ABSENT|LATE|HALF_DAY|HOLIDAY|LEAVE
    @Column(length = 500)
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
