package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_blends",
        uniqueConstraints = @UniqueConstraint(name = "uk_yrn_bld_org_code",
                columnNames = {"organization_id", "blend_code"}),
        indexes = @Index(name = "idx_yrn_bld_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YarnBlend extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blend_code", nullable = false, length = 30)
    private String blendCode;
    @Column(name = "blend_name", nullable = false, length = 200)
    private String blendName;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
