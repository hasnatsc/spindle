package com.asg.spindleserp.organization.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_cost_centers",
        indexes = {
                @Index(name = "idx_cc_bu", columnList = "business_unit_id"),
                @Index(name = "idx_cc_parent", columnList = "parent_cost_center_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_cost_center_id")
    private CostCenter parentCostCenter;

    @Column(nullable = false, unique = true, length = 50)
    private String costCenterCode;

    @Column(nullable = false, length = 200)
    private String costCenterName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CostCenter.CostCenterType costCenterType;

    @Column(length = 1000)
    private String description;
    @Column(length = 100)
    private String managerName;
    @Column(length = 100)
    private String managerEmail;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum CostCenterType {DEPARTMENT, PROJECT, BRANCH, DIVISION, PRODUCT, SERVICE}
}
