package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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

// =============================================================================
// EC REVIEW IMAGE
// =============================================================================

@Entity
@Table(name = "ec_review_images",
        indexes = @Index(name = "idx_ec_reviewimg_rev", columnList = "review_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReviewImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private EcReview review;

    @Column(nullable = false, length = 700) private String imageUrl;
    @Column(length = 700) private String thumbnailUrl;
    @Builder.Default private Integer displayOrder = 0;
}

// =============================================================================
// EC REVIEW VOTE  (helpful / not helpful)
// =============================================================================

@Entity
@Table(name = "ec_review_votes",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_review_vote",
                columnNames = {"review_id", "customer_id"}),
        indexes = @Index(name = "idx_ec_reviewvote_rev", columnList = "review_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReviewVote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private EcReview review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VoteType voteType;

    @Builder.Default private LocalDateTime votedAt = LocalDateTime.now();

    public enum VoteType { HELPFUL, NOT_HELPFUL }
}

// =============================================================================
// EC REVIEW SUMMARY  (materialized aggregate per product, updated by service)
// =============================================================================

@Entity
@Table(name = "ec_review_summary",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_review_summary",
                columnNames = "product_id"),
        indexes = {
                @Index(name = "idx_ec_reviewsum_prod", columnList = "product_id"),
                @Index(name = "idx_ec_reviewsum_org",  columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReviewSummary extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default @Column(precision = 3, scale = 2) private BigDecimal averageRating   = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false) private Integer totalReviews  = 0;
    @Builder.Default @Column(nullable = false) private Integer rating5        = 0;
    @Builder.Default @Column(nullable = false) private Integer rating4        = 0;
    @Builder.Default @Column(nullable = false) private Integer rating3        = 0;
    @Builder.Default @Column(nullable = false) private Integer rating2        = 0;
    @Builder.Default @Column(nullable = false) private Integer rating1        = 0;
    @Builder.Default @Column(precision = 5, scale = 2) private BigDecimal recommendationPct = BigDecimal.ZERO;
    private LocalDateTime updatedAt;
}

// =============================================================================
// EC PRODUCT QUESTION (Q&A)
// =============================================================================

@Entity
@Table(name = "ec_product_questions",
        indexes = {
                @Index(name = "idx_ec_qa_prod",   columnList = "product_id"),
                @Index(name = "idx_ec_qa_status", columnList = "question_status"),
                @Index(name = "idx_ec_qa_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcProductQuestion extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(length = 250) private String questionTitle;
    @Column(nullable = false, columnDefinition = "text") private String questionText;
    @Builder.Default @Column(nullable = false) private boolean anonymous = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus questionStatus = QuestionStatus.PENDING;

    @Builder.Default @Column(nullable = false) private Integer views = 0;
    @Builder.Default @Column(nullable = false) private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcProductAnswer> answers = new ArrayList<>();

    public enum QuestionStatus { PENDING, APPROVED, REJECTED, ANSWERED }
}

// =============================================================================
// EC PRODUCT ANSWER
// answered_by_user: ERP staff / admin (sec_users)
// answered_by_customer: community customer
// =============================================================================

@Entity
@Table(name = "ec_product_answers",
        indexes = @Index(name = "idx_ec_answer_q", columnList = "question_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcProductAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private EcProductQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_customer")
    private EcCustomer answeredByCustomer;

    // ERP staff answer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_user")
    private User answeredByUser;

    @Column(nullable = false, columnDefinition = "text") private String answerText;
    @Builder.Default @Column(nullable = false) private boolean officialAnswer = false;
    @Builder.Default @Column(nullable = false) private Integer helpfulCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerStatus answerStatus = AnswerStatus.APPROVED;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    public enum AnswerStatus { PENDING, APPROVED, REJECTED }
}
