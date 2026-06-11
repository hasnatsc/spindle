package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "yrn_counts",
        uniqueConstraints = @UniqueConstraint(name = "uk_yrn_cnt_org_code",
                columnNames = {"organization_id", "count_code"}),
        indexes = @Index(name = "idx_yrn_cnt_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YarnCount extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_code", nullable = false, length = 20)
    private String countCode;
    @Column(name = "count_name", nullable = false, length = 100)
    private String countName;
    @Column(name = "count_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal countValue;
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
    @Column(name = "approval_remarks", length = 500)
    private String approvalRemarks;
    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
