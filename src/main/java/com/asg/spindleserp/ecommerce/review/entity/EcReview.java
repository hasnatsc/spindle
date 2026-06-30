package com.asg.spindleserp.ecommerce.review.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.order.entity.EcOrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC REVIEW
// =============================================================================

@Entity
@Table(name = "ec_reviews",
        indexes = {
                @Index(name = "idx_ec_review_prod",   columnList = "product_id"),
                @Index(name = "idx_ec_review_cust",   columnList = "customer_id"),
                @Index(name = "idx_ec_review_rating", columnList = "rating"),
                @Index(name = "idx_ec_review_status", columnList = "review_status"),
                @Index(name = "idx_ec_review_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReview extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    // Link to the purchase — proves verified purchase status
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private EcOrderItem orderItem;

    @Column(nullable = false)
    private Integer rating;  // 1–5

    @Column(length = 200)  private String reviewTitle;
    @Column(columnDefinition = "text") private String reviewText;
    @Column(columnDefinition = "text") private String pros;
    @Column(columnDefinition = "text") private String cons;

    @Builder.Default @Column(nullable = false) private boolean verifiedPurchase = false;
    private Boolean recommendation;

    @Builder.Default @Column(nullable = false) private Integer likesCount    = 0;
    @Builder.Default @Column(nullable = false) private Integer dislikesCount = 0;
    @Builder.Default @Column(nullable = false) private Integer helpfulCount  = 0;
    @Builder.Default @Column(nullable = false) private Integer reportCount   = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;

    @Builder.Default @Column(nullable = false) private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcReviewImage> images = new ArrayList<>();

    public enum ReviewStatus { PENDING, APPROVED, REJECTED, HIDDEN }
}

