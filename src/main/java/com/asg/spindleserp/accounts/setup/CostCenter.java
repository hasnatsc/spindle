package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acc_cost_centers",
        uniqueConstraints = @UniqueConstraint(name = "uk_cc_org_code", columnNames = {"organization_id", "code"}),
        indexes = {
                @Index(name = "idx_cc_org", columnList = "organization_id"),
                @Index(name = "idx_cc_parent", columnList = "parent_cost_center_id"),
                @Index(name = "idx_cc_bu", columnList = "business_unit_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenter extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_cost_center_id")
    private CostCenter parent;

    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
