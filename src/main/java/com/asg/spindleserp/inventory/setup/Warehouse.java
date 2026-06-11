package com.asg.spindleserp.inventory.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_warehouses",
        uniqueConstraints = @UniqueConstraint(name = "uk_wh_org_code", columnNames = {"organization_id", "warehouse_code"}),
        indexes = {
                @Index(name = "idx_wh_org", columnList = "organization_id"),
                @Index(name = "idx_wh_type", columnList = "item_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;

    @Column(name = "warehouse_code", nullable = false, length = 50)
    private String warehouseCode;
    @Column(name = "warehouse_name", nullable = false, length = 200)
    private String warehouseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(name = "manager_name", length = 100)
    private String managerName;
    @Column(name = "contact_number", length = 20)
    private String contactNumber;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
