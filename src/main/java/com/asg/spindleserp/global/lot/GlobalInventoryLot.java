package com.asg.spindleserp.global.lot;

import com.asg.spindleserp.accounts.setup.Bank;
import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.locations.Country;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Certification;
import com.asg.spindleserp.inventory.setup.ColorGrade;
import com.asg.spindleserp.inventory.setup.InventoryLotStatus;
import com.asg.spindleserp.inventory.setup.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "global_inv_lots",
        uniqueConstraints = @UniqueConstraint(name = "uk_inv_lot_org_no", columnNames = {"organization_id", "lot_number"}),
        indexes = {
                @Index(name = "idx_lot_item", columnList = "item_id"),
                @Index(name = "idx_lot_status", columnList = "status"),
                @Index(name = "idx_lot_expiry", columnList = "expiry_date"),
                @Index(name = "idx_lot_received", columnList = "received_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalInventoryLot extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_number", nullable = false, length = 100)
    private String lotNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private SubAccount supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_of_origin_id")
    private Country countryOfOrigin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    // Dates
    @Column(name = "received_date")
    private LocalDate receivedDate;
    @Column(name = "production_date")
    private LocalDate productionDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    // Classification
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Certification certification;
    @Enumerated(EnumType.STRING)
    @Column(name = "color_grade", length = 50)
    private ColorGrade colorGrade;

    // Fiber quality
    @Column(name = "avg_staple_length", precision = 8, scale = 3)
    private BigDecimal avgStapleLength;
    @Column(name = "avg_micronaire", precision = 8, scale = 3)
    private BigDecimal avgMicronaire;
    @Column(name = "avg_moisture", precision = 8, scale = 3)
    private BigDecimal avgMoisture;
    @Column(name = "avg_trash_percent", precision = 5, scale = 3)
    private BigDecimal avgTrashPercent;
    @Column(name = "avg_purity", precision = 5, scale = 3)
    private BigDecimal avgPurity;
    @Column(precision = 8, scale = 3)
    private BigDecimal denier;

    // Location
    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;
    @Column(name = "shelf_location", length = 100)
    private String shelfLocation;
    @Column(name = "bin_location", length = 100)
    private String binLocation;

    // Batch
    @Column(name = "batch_no", length = 100)
    private String batchNo;
    @Column(name = "manufacturer_batch_no", length = 100)
    private String manufacturerBatchNo;
    @Column(precision = 8, scale = 3)
    private BigDecimal concentration;
    @Column(name = "chemical_grade", length = 50)
    private String chemicalGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InventoryLotStatus status = InventoryLotStatus.AVAILABLE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
