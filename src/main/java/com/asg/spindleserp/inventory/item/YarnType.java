package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_types",
        uniqueConstraints = @UniqueConstraint(name = "uk_yrn_type_org_code", columnNames = {"organization_id", "type_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YarnType extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "type_code", nullable = false, length = 30)
    private String typeCode;
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;
    @Column(name = "type_name_short", length = 30)
    private String typeNameShort;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
