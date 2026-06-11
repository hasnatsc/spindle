package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eco_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_prod_cust_order",
                columnNames = {"product_id", "customer_id", "eco_order_id"}),
        indexes = {
                @Index(name = "idx_review_prod", columnList = "product_id"),
                @Index(name = "idx_review_cust", columnList = "customer_id"),
                @Index(name = "idx_review_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoReview extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eco_order_id")
    private EcoOrder ecoOrder;
    @Column(nullable = false)
    private Integer rating;  // 1–5  (enforced by CHECK constraint in DB)
    @Column(length = 200)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String body;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    // PENDING|APPROVED|REJECTED|SPAM
    @Builder.Default
    @Column(name = "is_verified_purchase", nullable = false)
    private Boolean isVerifiedPurchase = false;
    @Builder.Default
    @Column(name = "helpful_count", nullable = false)
    private Integer helpfulCount = 0;
    @Builder.Default
    @Column(name = "not_helpful_count", nullable = false)
    private Integer notHelpfulCount = 0;
    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;
    @Column(name = "admin_replied_at")
    private LocalDateTime adminRepliedAt;
    @Column(name = "admin_replied_by", length = 100)
    private String adminRepliedBy;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
