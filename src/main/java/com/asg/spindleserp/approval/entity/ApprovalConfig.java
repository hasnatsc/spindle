package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apr_configs",
        uniqueConstraints = @UniqueConstraint(name = "uq_aprc_code", columnNames = "code"),
        indexes = @Index(name = "idx_aprc_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, length = 50)
    private String documentType;
    @Column(nullable = false, length = 30)
    private String module;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String flowType = "SEQUENTIAL"; // SEQUENTIAL | PARALLEL

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean enableReminders = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean useReportingHierarchy = false;

    private Integer priority;
    private Integer autoEscalationHours;
    private Integer reminderIntervalHours;

    @Column(precision = 18, scale = 2)
    private BigDecimal minAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal maxAmount;

    @Builder.Default
    @OneToMany(mappedBy = "approvalConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalLevel> levels = new ArrayList<>();
}
