package com.asg.spindleserp.ecommerce.shipping;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_shipping_methods",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_ship_method",
                columnNames = {"organization_id", "method_code"}),
        indexes = @Index(name = "idx_ec_shipmethod_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcShippingMethod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String methodCode;
    @Column(length = 100)
    private String methodName;
    @Column(length = 100)
    private String courierName;
    private Integer estimatedDays;
    @Column(precision = 18, scale = 2)
    private BigDecimal baseCharge;
    @Column(precision = 18, scale = 2)
    private BigDecimal chargePerKg;

    @Builder.Default
    @Column(nullable = false)
    private boolean cashOnDelivery = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean apiEnabled = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
