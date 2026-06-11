package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fa_assets",
        uniqueConstraints = @UniqueConstraint(name = "uk_fa_org_code", columnNames = {"organization_id", "asset_code"}),
        indexes = {
                @Index(name = "idx_fa_org", columnList = "organization_id"),
                @Index(name = "idx_fa_category", columnList = "asset_category_id"),
                @Index(name = "idx_fa_status", columnList = "status"),
                @Index(name = "idx_fa_dept", columnList = "department_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAsset extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private InventoryItem linkedItem;
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
    private SubAccount supplier;

    @Column(name = "asset_code", nullable = false, length = 50)
    private String assetCode;
    @Column(name = "asset_name", nullable = false, length = 200)
    private String assetName;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    @Column(length = 100)
    private String model;
    @Column(length = 100)
    private String manufacturer;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;
    @Column(name = "capitalisation_date")
    private LocalDate capitalisationDate;
    @Column(name = "purchase_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal purchaseCost;
    @Builder.Default
    @Column(name = "installation_cost", precision = 18, scale = 2)
    private BigDecimal installationCost = BigDecimal.ZERO;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";

    @Column(name = "depreciation_method", nullable = false, length = 30)
    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE"; // STRAIGHT_LINE|DECLINING_BALANCE|UNITS_OF_PRODUCTION

    @Column(name = "useful_life_years")
    private Integer usefulLifeYears;
    @Builder.Default
    @Column(name = "residual_value", precision = 18, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;
    @Column(name = "depreciation_rate", precision = 5, scale = 2)
    private BigDecimal depreciationRate;
    @Column(name = "depreciation_start_date")
    private LocalDate depreciationStartDate;
    @Builder.Default
    @Column(name = "accumulated_depreciation", nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;
    @Column(name = "current_book_value", precision = 18, scale = 2)
    private BigDecimal currentBookValue;
    @Column(name = "last_dep_run_date")
    private LocalDate lastDepRunDate;

    @Column(length = 200)
    private String location;
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE|DISPOSED|TRANSFERRED|SOLD|WRITTEN_OFF|UNDER_MAINTENANCE

    @Column(length = 20)
    @Builder.Default
    private String condition = "GOOD";
    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;
    @Column(name = "insurance_policy_no", length = 100)
    private String insurancePolicyNo;
    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;
    @Column(length = 100)
    private String barcode;
    @Column(name = "qr_code", length = 100)
    private String qrCode;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Computed book value = total cost − accumulated depreciation
     */
    @PostLoad
    public void computeBookValue() {
        BigDecimal total = (purchaseCost != null ? purchaseCost : BigDecimal.ZERO)
                .add(installationCost != null ? installationCost : BigDecimal.ZERO);
        this.currentBookValue = total.subtract(
                accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO);
    }
}
