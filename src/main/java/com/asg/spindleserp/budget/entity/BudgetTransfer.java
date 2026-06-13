package com.asg.spindleserp.budget.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_transfers",
        uniqueConstraints = @UniqueConstraint(name = "uq_bt_org_no",
                columnNames = {"organization_id", "transfer_no"}),
        indexes = @Index(name = "idx_bt_budget", columnList = "budget_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_line_id", nullable = false)
    private BudgetLine fromLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_line_id", nullable = false)
    private BudgetLine toLine;

    @Column(nullable = false, length = 50)
    private String transferNo;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal transferAmount;
    @Column(nullable = false, columnDefinition = "text")
    private String reason;
    @Column(nullable = false)
    private LocalDate transferDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetTransfer.TransferStatus status = BudgetTransfer.TransferStatus.PENDING;

    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransferStatus {PENDING, APPROVED, REJECTED}
}
