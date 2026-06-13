package com.asg.spindleserp.global.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_business_document_line_lots",
        indexes = {
                @Index(name = "idx_gbdll_line", columnList = "document_line_id"),
                @Index(name = "idx_gbdll_lot", columnList = "lot_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDocumentLineLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_line_id", nullable = false)
    private BusinessDocumentLine documentLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryLot lot;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
    @Column(precision = 12, scale = 3)
    private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3)
    private BigDecimal netWeight;
    @Column(precision = 18, scale = 4)
    private BigDecimal unitCost;
    @Column(precision = 18, scale = 2)
    private BigDecimal totalCost;
    @Column(columnDefinition = "text")
    private String remarks;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
