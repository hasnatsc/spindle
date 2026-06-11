package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eco_shipment_tracking",
        indexes = @Index(name = "idx_strack_ship", columnList = "shipment_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoShipmentTracking implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private EcoShipment shipment;
    @Column(name = "event_status", nullable = false, length = 50)
    private String eventStatus;
    @Column(name = "event_location", length = 200)
    private String eventLocation;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    @Column(length = 20)
    @Builder.Default
    private String source = "SYSTEM"; // SYSTEM|COURIER_API|MANUAL
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
