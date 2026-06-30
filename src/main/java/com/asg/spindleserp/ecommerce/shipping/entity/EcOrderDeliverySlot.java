package com.asg.spindleserp.ecommerce.shipping.entity;

import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ec_order_delivery_slots",
        indexes = {
                @Index(name = "idx_ec_orderslot_order", columnList = "order_id"),
                @Index(name = "idx_ec_orderslot_date", columnList = "delivery_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrderDeliverySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_slot_id", nullable = false)
    private EcDeliverySlot deliverySlot;

    @Column(nullable = false)
    private LocalDate deliveryDate;
    @Builder.Default
    @Column(nullable = false)
    private boolean confirmed = false;
}
