package com.asg.spindleserp.ecommerce.shipping.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "ec_delivery_slots",
        indexes = @Index(name = "idx_ec_slot_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcDeliverySlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maximumOrders;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
