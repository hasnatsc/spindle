package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "eco_shipping_zones",
        uniqueConstraints = @UniqueConstraint(name = "uk_sz_store_name", columnNames = {"store_id", "name"}),
        indexes = @Index(name = "idx_sz_store", columnList = "store_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoShippingZone implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String countries;
    @Column(columnDefinition = "TEXT")
    private String districts;
    @Column(name = "postal_codes", columnDefinition = "TEXT")
    private String postalCodes;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
