package com.asg.spindleserp.production.order;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "prd_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_prd_org_no", columnNames = {"organization_id", "order_no"}),
        indexes = {
                @Index(name = "idx_prd_org", columnList = "organization_id"),
                @Index(name = "idx_prd_status", columnList = "status"),
                @Index(name = "idx_prd_yarn", columnList = "yarn_product_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrder extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_management_id", nullable = false)
    private BusinessDocument orderManagement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_product_id", nullable = false)
    private InventoryItem yarnProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;
    @Column(name = "planned_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal plannedQuantity;
    @Builder.Default
    @Column(name = "produced_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal producedQuantity = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "waste_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal wasteQuantity = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "total_bags_packed", nullable = false)
    private Long totalBagsPacked = 0L;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;
    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "planned_cost_per_kg", precision = 14, scale = 4)
    private BigDecimal plannedCostPerKg;
    @Column(name = "actual_cost_per_kg", precision = 14, scale = 4)
    private BigDecimal actualCostPerKg;
    @Column(name = "total_planned_cost", precision = 18, scale = 2)
    private BigDecimal totalPlannedCost;
    @Column(name = "total_actual_cost", precision = 18, scale = 2)
    private BigDecimal totalActualCost;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";
    // DRAFT|SUBMITTED|APPROVED|IN_PROGRESS|COMPLETED|CANCELLED|REJECTED

    @Column(name = "approval_status", length = 30)
    @Builder.Default
    private String approvalStatus = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
