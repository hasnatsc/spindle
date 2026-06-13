package com.asg.spindleserp.global.entity;

import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inv_lots",
        indexes = {
                @Index(name = "idx_lot_item", columnList = "item_id"),
                @Index(name = "idx_lot_org", columnList = "organization_id"),
                @Index(name = "idx_lot_status", columnList = "status"),
                @Index(name = "idx_lot_number", columnList = "lot_number"),
                @Index(name = "idx_lot_deleted", columnList = "deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Soft FKs (resolved at service layer to avoid circular deps)
    private Long countryOfOriginId;
    private Long bankId;
    private Long supplierId;           // → acc_chart_of_accounts_sub
    private Long productionOrderId;    // ★ replaces productionRecipeId → prd_productions (DEFERRED)

    @Version
    @Column(name = "version")
    private Long version;              // optimistic lock

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, length = 100)
    private String lotNumber;

    // ★ Generic item type (no FIBER/YARN/FABRICS)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType = ItemType.GENERAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private InventoryLot.LotStatus status = InventoryLot.LotStatus.AVAILABLE;

    // ── Generic lot attributes ─────────────────────────────────────────────
    @Column(length = 100)
    private String batchNo;
    @Column(length = 100)
    private String manufacturerBatchNo;
    @Column(length = 100)
    private String serialNo;
    @Column(length = 100)
    private String binLocation;
    @Column(length = 100)
    private String shelfLocation;
    @Column(length = 100)
    private String warehouseLocation;

    // ── Dates ─────────────────────────────────────────────────────────────
    private LocalDate receivedDate;
    private LocalDate manufacturingDate;
    private LocalDate productionDate;
    private LocalDate expiryDate;

    // ★ GENERIC QC attributes (replaces fiber-specific micronaire/staple_length/denier/avg_*)
    @Column(length = 50)
    private String qcGrade;    // A, B, C / PASS / FAIL / etc.
    @Column(columnDefinition = "text")
    private String qcRemarks;
    private Boolean qcPassed;
    private LocalDate qcDate;
    @Column(length = 100)
    private String qcBy;

    // ── Physical ──────────────────────────────────────────────────────────
    @Column(precision = 12, scale = 3)
    private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3)
    private BigDecimal netWeight;
    @Column(precision = 18, scale = 4)
    private BigDecimal unitCost;  // cost per unit when received

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

    public enum LotStatus {AVAILABLE, RESERVED, BLOCKED, QC_HOLD, EXPIRED, CONSUMED}
}
