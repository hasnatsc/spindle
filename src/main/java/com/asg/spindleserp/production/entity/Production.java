package com.asg.spindleserp.production.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.approval.entity.ApprovalRequest;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.inventory.entity.ItemUom;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Production Work Order.
 * <p>
 * Contains both the work order header AND a built-in cost sheet.
 * The cost sheet has 4 cost components:
 * materialCost  = auto-summed from ProductionInput.totalCost when production is completed
 * laborCost     = pulled from hrm_cost_center_allocations for this costCenter + month
 * overheadCost  = manual entry or overhead rate allocation
 * otherCost     = packaging, freight, utilities, etc.
 * totalCost     = materialCost + laborCost + overheadCost + otherCost
 * unitCost      = totalCost / producedQuantity
 * <p>
 * unitCost is then copied to each ProductionOutput.unitCost for COGS calculation on sale.
 */
@Entity
@Table(name = "prd_productions",
        uniqueConstraints = @UniqueConstraint(name = "uq_prd2_org_no",
                columnNames = {"organization_id", "production_no"}),
        indexes = {
                @Index(name = "idx_prd2_org", columnList = "organization_id"),
                @Index(name = "idx_prd2_status", columnList = "status"),
                @Index(name = "idx_prd2_item", columnList = "finished_item_id"),
                @Index(name = "idx_prd2_date", columnList = "production_date"),
                @Index(name = "idx_prd2_bom", columnList = "bom_id"),
                @Index(name = "idx_prd2_so", columnList = "sales_order_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Production extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Optional — pre-fills inputs from BOM template
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id")
    private Bom bom;

    /**
     * The finished product being manufactured
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    /**
     * Where finished goods will be stored after production
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_warehouse_id", nullable = false)
    private Warehouse outputWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    /**
     * Optional link to Sales Order that triggered this production
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private BusinessDocument salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    /**
     * Journal entry created when production is posted to GL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_unit_id", nullable = false)
    private ItemUom outputUnit;

    // ── Work Order Header ──────────────────────────────────────────────────
    @Column(nullable = false, length = 50)
    private String productionNo;
    @Column(nullable = false)
    private LocalDate productionDate;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    // ── Quantities ─────────────────────────────────────────────────────────
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal plannedQuantity;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal producedQuantity = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal wasteQuantity = BigDecimal.ZERO;

    // ── COST SHEET ─────────────────────────────────────────────────────────
    // materialCost: sum of ProductionInput.totalCost after completion
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal materialCost = BigDecimal.ZERO;

    // laborCost: from hrm_cost_center_allocations (costCenter + allocationMonth)
    // Proportional to this order's produced_quantity vs total monthly production
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;

    // overheadCost: factory rent, electricity, depreciation — manual or allocated
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal overheadCost = BigDecimal.ZERO;

    // otherCost: packaging, transport, quality testing, etc.
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal otherCost = BigDecimal.ZERO;

    // totalCost = materialCost + laborCost + overheadCost + otherCost
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    // unitCost = totalCost / producedQuantity — used as COGS basis when goods are sold
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    // ── Status ─────────────────────────────────────────────────────────────
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Production.ProductionStatus status = Production.ProductionStatus.DRAFT;

    @Column(length = 30)
    private String approvalStatus;
    @Column(columnDefinition = "text")
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionInput> inputs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionOutput> outputs = new ArrayList<>();

    public enum ProductionStatus {
        DRAFT, SUBMITTED, APPROVED, RELEASED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
    }
}
