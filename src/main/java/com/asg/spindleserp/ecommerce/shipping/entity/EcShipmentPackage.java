package com.asg.spindleserp.ecommerce.shipping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_shipment_packages",
        indexes = @Index(name = "idx_ec_package_ship", columnList = "shipping_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcShipmentPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_id", nullable = false)
    private EcShipping shipping;

    @Column(length = 50)
    private String packageNo;
    @Column(precision = 12, scale = 3)
    private BigDecimal weight;
    @Column(precision = 12, scale = 3)
    private BigDecimal length;
    @Column(precision = 12, scale = 3)
    private BigDecimal width;
    @Column(precision = 12, scale = 3)
    private BigDecimal height;
    private Integer itemCount;
    @Column(columnDefinition = "text")
    private String remarks;
}
