package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_alerts",
        indexes = {
                @Index(name = "idx_bal_budget", columnList = "budget_id"),
                @Index(name = "idx_bal_type", columnList = "alert_type"),
                @Index(name = "idx_bal_user", columnList = "notify_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_line_id")
    private BudgetLine budgetLine;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notify_user_id")
    private User notifyUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetAlert.AlertType alertType;

    @Column(precision = 5, scale = 2)
    private BigDecimal thresholdPct;
    @Column(columnDefinition = "text")
    private String message;
    private LocalDateTime triggeredAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean isResolved = false;
    private LocalDateTime resolvedAt;
    @Column(length = 100)
    private String resolvedBy;

    @Builder.Default
    @Column(nullable = false)
    private boolean notificationSent = false;
    private LocalDateTime sentAt;
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

    public enum AlertType {THRESHOLD_WARNING, OVER_BUDGET, ENCUMBRANCE_EXPIRY, BUDGET_EXPIRY}
}
