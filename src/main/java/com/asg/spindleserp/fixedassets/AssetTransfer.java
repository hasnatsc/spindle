package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_asset_transfers",
        indexes = @Index(name = "idx_fatrf_asset", columnList = "asset_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetTransfer extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_department_id")
    private Department fromDepartment;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_department_id")
    private Department toDepartment;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_cost_center_id")
    private CostCenter fromCostCenter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_cost_center_id")
    private CostCenter toCostCenter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_warehouse_id")
    private Warehouse toWarehouse;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_employee_id")
    private Employee fromEmployee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_employee_id")
    private Employee toEmployee;

    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
