package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_encumbrances",
        indexes = {
                @Index(name = "idx_be_budget", columnList = "budget_id"),
                @Index(name = "idx_be_line", columnList = "budget_line_id"),
                @Index(name = "idx_be_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encumbrance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private BusinessDocument sourceDocument;

    @Column(nullable = false, length = 50)
    private String sourceDocumentType;
    @Column(nullable = false, length = 100)
    private String sourceDocumentNo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal committedAmount;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    // outstandingAmount = committedAmount - releasedAmount
    // GENERATED ALWAYS AS STORED in DB — NOT mapped here. Read via DTO.

    @Column(nullable = false)
    private LocalDate commitmentDate;
    private LocalDate expectedInvoiceDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Encumbrance.EncumbranceStatus status = Encumbrance.EncumbranceStatus.OPEN;

    @Column(columnDefinition = "text")
    private String notes;
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

    public enum EncumbranceStatus {OPEN, PARTIAL, FULLY_RELEASED, CANCELLED}
}
