package com.asg.spindleserp.ecommerce.customerSupport;

import com.asg.spindleserp.ecommerce.EcCustomer;
import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_recently_viewed",
        indexes = {
                @Index(name = "idx_ec_recent_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_recent_prod", columnList = "product_id"),
                @Index(name = "idx_ec_recent_time", columnList = "viewed_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcRecentlyViewed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default
    private LocalDateTime viewedAt = LocalDateTime.now();
    @Column(length = 50)
    private String ipAddress;
    @Column(length = 255)
    private String deviceInfo;
}
