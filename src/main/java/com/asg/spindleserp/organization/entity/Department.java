package com.asg.spindleserp.organization.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_departments",
        indexes = {
                @Index(name = "idx_dept_org", columnList = "organization_id"),
                @Index(name = "idx_dept_parent", columnList = "parent_department_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    // Deferred FK to hrm_employees.id — stored as plain Long to avoid circular dep
    @Column(name = "head_employee_id")
    private Long headEmployeeId;

    @Column(unique = true, length = 50)
    private String code;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
