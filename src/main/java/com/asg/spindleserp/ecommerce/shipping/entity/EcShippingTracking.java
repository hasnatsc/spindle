package com.asg.spindleserp.ecommerce.shipping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_shipping_tracking",
        indexes = {
                @Index(name = "idx_ec_tracking_ship", columnList = "shipping_id"),
                @Index(name = "idx_ec_tracking_time", columnList = "event_time")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcShippingTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_id", nullable = false)
    private EcShipping shipping;

    @Column(length = 50)
    private String trackingStatus;
    @Column(length = 200)
    private String trackingLocation;
    private LocalDateTime eventTime;
    @Column(columnDefinition = "text")
    private String remarks;
}
