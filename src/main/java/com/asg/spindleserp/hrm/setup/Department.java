package com.asg.spindleserp.hrm.setup;

import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "org_departments",
        uniqueConstraints = @UniqueConstraint(name = "uk_dept_org_name", columnNames = {"organization_id", "name"}),
        indexes = {
                @Index(name = "idx_dept_org", columnList = "organization_id"),
                @Index(name = "idx_dept_parent", columnList = "parent_department_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"employees", "parentDepartment", "departmentHead"})
public class Department implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    /**
     * Deferred circular FK: Department.head_employee_id → hrm_employees.id
     * Set AFTER employee is created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_employee_id")
    private Employee departmentHead;

    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 50)
    private String code;
    @Column(length = 500)
    private String description;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<Employee> employees = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void assignOrganizationOnCreate() {
        if (organization == null) organization = ContextProvider.getOrganizationReference();
    }

    public int getEmployeeCount() {
        return employees != null ? employees.size() : 0;
    }
}
