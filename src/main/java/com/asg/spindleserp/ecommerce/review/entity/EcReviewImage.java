package com.asg.spindleserp.ecommerce.review.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_review_images",
        indexes = @Index(name = "idx_ec_reviewimg_rev", columnList = "review_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private EcReview review;

    @Column(nullable = false, length = 700)
    private String imageUrl;
    @Column(length = 700)
    private String thumbnailUrl;
    @Builder.Default
    private Integer displayOrder = 0;
}
