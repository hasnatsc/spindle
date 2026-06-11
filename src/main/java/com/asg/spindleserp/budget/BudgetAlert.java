package com.asg.spindleserp.budget;

import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import lombok.Getter;

@Entity
@Table(name = "bgt_alerts",
        indexes = {
                @Index(name = "idx_bgt_alert_budget", columnList = "budget_id"),
                @Index(name = "idx_bgt_alert_user", columnList = "notify_user_id"),
                @Index(name = "idx_bgt_alert_unsent", columnList = "notification_sent,triggered_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAlert implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_line_id")
    private BudgetLine budgetLine;   // NULL = entire budget

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notify_user_id")
    private User notifyUser;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;
    // THRESHOLD_WARNING | OVER_BUDGET | ENCUMBRANCE_EXPIRY | BUDGET_EXPIRY

    @Column(name = "threshold_pct", precision = 5, scale = 2)
    private BigDecimal thresholdPct;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;
    @Builder.Default
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
    @Builder.Default
    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isOverBudget() {
        return "OVER_BUDGET".equals(alertType);
    }

    public boolean isThresholdWarning() {
        return "THRESHOLD_WARNING".equals(alertType);
    }

    public boolean isPending() {
        return !isResolved && !notificationSent;
    }

    public void markSent() {
        this.notificationSent = true;
        this.sentAt = LocalDateTime.now();
    }

    public void resolve(String resolvedByUser) {
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedByUser;
    }
}
