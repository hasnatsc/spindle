package com.asg.spindleserp.inventory.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_units_of_measure",
        uniqueConstraints = @UniqueConstraint(name = "uk_uom_org_code", columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitsOfMeasure extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 20)
    private String symbol;
    @Column(name = "uom_category", nullable = false, length = 30)
    private String uomCategory; // WEIGHT|COUNT|LENGTH|VOLUME|AREA
    @Builder.Default
    @Column(name = "is_base_unit", nullable = false)
    private Boolean isBaseUnit = false;
    @Builder.Default
    @Column(name = "conversion_factor", nullable = false, precision = 12, scale = 6)
    private BigDecimal conversionFactor = BigDecimal.ONE;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
