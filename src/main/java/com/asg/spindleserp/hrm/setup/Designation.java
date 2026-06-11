package com.asg.spindleserp.hrm.setup;

import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hrm_designations",
        uniqueConstraints = @UniqueConstraint(name = "uk_desig_org_title", columnNames = {"organization_id", "title"}),
        indexes = @Index(name = "idx_desig_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"employees"})
public class Designation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String title;
    @Column(length = 50)
    private String code;
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer level;  // 1 = CEO (highest), 10 = entry level

    @Column(name = "min_salary", precision = 12, scale = 2)
    private BigDecimal minSalary;
    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "designation", cascade = CascadeType.ALL)
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
