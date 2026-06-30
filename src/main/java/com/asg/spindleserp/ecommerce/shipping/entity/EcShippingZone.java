package com.asg.spindleserp.ecommerce.shipping.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_shipping_zones",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_ship_zone",
                columnNames = {"organization_id", "zone_code"}),
        indexes = @Index(name = "idx_ec_shipzone_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcShippingZone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String zoneCode;
    @Column(length = 150)
    private String zoneName;
    @Builder.Default
    @Column(length = 100)
    private String country = "Bangladesh";
    @Column(length = 100)
    private String division;
    @Column(length = 100)
    private String district;
    @Column(precision = 18, scale = 2)
    private BigDecimal shippingCharge;
    @Column(precision = 18, scale = 2)
    private BigDecimal minimumOrderAmount;
    @Builder.Default
    @Column(nullable = false)
    private boolean freeShipping = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
