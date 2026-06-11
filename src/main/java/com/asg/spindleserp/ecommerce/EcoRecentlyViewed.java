package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eco_recently_viewed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoRecentlyViewed implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private LocalDateTime viewedAt;
}
