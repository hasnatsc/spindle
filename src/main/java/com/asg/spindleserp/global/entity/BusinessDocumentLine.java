package com.asg.spindleserp.global.entity;

import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_document_lines",
        indexes = {
                @Index(name = "idx_gbdl_doc", columnList = "document_id"),
                @Index(name = "idx_gbdl_item", columnList = "item_id"),
                @Index(name = "idx_gbdl_lot", columnList = "inventory_lot_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDocumentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private BusinessDocument document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private InventoryLot inventoryLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_line_id")
    private BusinessDocumentLine sourceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(length = 100)
    private String itemCode;
    @Column(length = 500)
    private String itemName;
    @Column(length = 1000)
    private String description;
    @Column(length = 20)
    private String unitCode;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
    @Column(precision = 18, scale = 3)
    private BigDecimal deliveredQty;
    @Column(precision = 18, scale = 3)
    private BigDecimal receivedQty;
    @Column(precision = 18, scale = 3)
    private BigDecimal acceptedQty;
    @Column(precision = 18, scale = 3)
    private BigDecimal rejectedQty;
    @Column(precision = 18, scale = 4)
    private BigDecimal unitPrice;
    @Column(precision = 18, scale = 2)
    private BigDecimal discountAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal taxAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal lineAmount;

    @Column(length = 100)
    private String batchNumber;
    private LocalDate expectedDate;
    @Column(length = 30)
    private String qualityStatus;
    @Column(columnDefinition = "text")
    private String qualityRemarks;
    @Column(columnDefinition = "text")
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(length = 100)
    private String createdBy;
    @Column(length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder.Default
    @OneToMany(mappedBy = "documentLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessDocumentLineLot> lots = new ArrayList<>();
}
