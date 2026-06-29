package com.asg.spindleserp.ecommerce.review;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_review_summary",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_review_summary",
                columnNames = "product_id"),
        indexes = {
                @Index(name = "idx_ec_reviewsum_prod", columnList = "product_id"),
                @Index(name = "idx_ec_reviewsum_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReviewSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false)
    private Integer totalReviews = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer rating5 = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer rating4 = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer rating3 = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer rating2 = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer rating1 = 0;
    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal recommendationPct = BigDecimal.ZERO;
    private LocalDateTime updatedAt;
}
