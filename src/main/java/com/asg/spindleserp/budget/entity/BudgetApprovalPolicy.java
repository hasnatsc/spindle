package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.approval.entity.ApprovalConfig;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_approval_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetApprovalPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_config_id")
    private ApprovalConfig approvalConfig;

    @Column(nullable = false, length = 30)
    private String budgetType;
    @Column(precision = 18, scale = 2)
    private BigDecimal minAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal maxAmount;

    @Builder.Default
    @Column(nullable = false)
    private boolean requireCfo = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean requireMd = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
