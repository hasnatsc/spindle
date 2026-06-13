package com.asg.spindleserp.fixedassets.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.hrm.entity.Employee;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.entity.Department;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fa_assets",
        uniqueConstraints = @UniqueConstraint(name = "uq_fa_org_code",
                columnNames = {"organization_id", "asset_code"}),
        indexes = {
                @Index(name = "idx_fa_org", columnList = "organization_id"),
                @Index(name = "idx_fa_cat", columnList = "asset_category_id"),
                @Index(name = "idx_fa_status", columnList = "status"),
                @Index(name = "idx_fa_dept", columnList = "department_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_category_id", nullable = false)
    private AssetCategory assetCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_item_id")
    private Item linkedItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_grn_id")
    private BusinessDocument linkedGrn;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_po_id")
    private BusinessDocument linkedPo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_employee_id")
    private Employee responsibleEmployee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private ChartOfAccountSub supplier;

    @Column(nullable = false, length = 50)
    private String assetCode;
    @Column(nullable = false, length = 200)
    private String assetName;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 100)
    private String serialNumber;
    @Column(length = 100)
    private String model;
    @Column(length = 100)
    private String manufacturer;

    @Column(nullable = false)
    private LocalDate acquisitionDate;
    private LocalDate capitalisationDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal purchaseCost;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal installationCost = BigDecimal.ZERO;

    // totalCost = purchaseCost + installationCost
    // GENERATED ALWAYS AS STORED in DB — NOT mapped here.
    // Access via: @Formula("purchase_cost + installation_cost") or native query.

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "BDT";
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetCategory.DepreciationMethod depreciationMethod
            = AssetCategory.DepreciationMethod.STRAIGHT_LINE;

    private Integer usefulLifeYears;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal depreciationRate;
    private LocalDate depreciationStartDate;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal currentBookValue;
    private LocalDate lastDepRunDate;

    @Column(length = 200)
    private String location;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Asset.AssetStatus status = Asset.AssetStatus.ACTIVE;

    @Builder.Default
    @Column(length = 20)
    private String condition = "GOOD";

    private LocalDate warrantyExpiryDate;
    @Column(length = 100)
    private String insurancePolicyNo;
    private LocalDate insuranceExpiryDate;
    @Column(length = 100)
    private String barcode;
    @Column(columnDefinition = "text")
    private String notes;

    public enum AssetStatus {
        ACTIVE, DISPOSED, TRANSFERRED, SOLD, WRITTEN_OFF, UNDER_MAINTENANCE
    }
}
