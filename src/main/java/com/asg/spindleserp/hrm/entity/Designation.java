package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hrm_designations",
        uniqueConstraints = @UniqueConstraint(name = "uq_desig_org_code",
                columnNames = {"organization_id", "designation_code"}),
        indexes = @Index(name = "idx_desig_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Designation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String designationCode;
    @Column(nullable = false, length = 200)
    private String designationName;
    @Column(length = 20)
    private String grade;
    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
