package com.asg.spindleserp.approval;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "apr_configs",
        uniqueConstraints = @UniqueConstraint(name = "uk_apr_cfg", columnNames = {"organization_id", "document_type", "code"}),
        indexes = @Index(name = "idx_apr_cfg_dtype", columnList = "document_type"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalConfig extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(nullable = false, length = 50)
    private String module;
    @Column(name = "flow_type", nullable = false, length = 20)
    @Builder.Default
    private String flowType = "SEQUENTIAL"; // SEQUENTIAL|PARALLEL

    @Column(name = "min_amount", precision = 18, scale = 2)
    private BigDecimal minAmount;
    @Column(name = "max_amount", precision = 18, scale = 2)
    private BigDecimal maxAmount;
    @Builder.Default
    @Column(name = "use_reporting_hierarchy", nullable = false)
    private Boolean useReportingHierarchy = false;
    @Column(name = "auto_escalation_hours")
    private Integer autoEscalationHours;
    @Builder.Default
    @Column(name = "enable_reminders", nullable = false)
    private Boolean enableReminders = true;
    @Builder.Default
    @Column(name = "reminder_interval_hours")
    private Integer reminderIntervalHours = 24;
    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 100;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "approvalConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("levelNumber ASC")
    @Builder.Default
    private List<ApprovalLevel> approvalLevels = new ArrayList<>();

    public int getTotalLevels() {
        return approvalLevels.size();
    }
}
