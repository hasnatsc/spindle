
package com.asg.spindleserp.organization.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.common.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_warehouses",
    indexes = @Index(name = "idx_wh_bu", columnList = "business_unit_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;

    @Column(nullable = false, unique = true, length = 50)
    private String warehouseCode;

    @Column(nullable = false, length = 200)
    private String warehouseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType;

    @Column(columnDefinition = "text") private String address;
    @Column(length = 100) private String managerName;
    @Column(length = 20)  private String contactNumber;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
