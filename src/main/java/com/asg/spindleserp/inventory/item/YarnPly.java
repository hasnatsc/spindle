
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_plies",
    uniqueConstraints = @UniqueConstraint(name = "uk_yrn_ply_org_num",
        columnNames = {"organization_id","ply_number"}),
    indexes = @Index(name = "idx_yrn_ply_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnPly extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ply_number", nullable = false) private Integer plyNumber;
    @Column(name = "ply_code",   nullable = false, length = 20) private String plyCode;
    @Column(name = "ply_name",   nullable = false, length = 50) private String plyName;
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_approved", nullable = false) private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at") private java.time.LocalDateTime approvedAt;
    @Column(name = "created_by",  length = 100) private String createdBy;
    @Column(name = "updated_by",  length = 100) private String updatedBy;

    /** Display label used by YarnItem.buildDisplayName() */
    public String getDisplayName() {
        return plyCode != null ? plyCode : String.valueOf(plyNumber);
    }
}