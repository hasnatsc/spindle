package com.asg.spindleserp.ecommerce.review;

import com.asg.spindleserp.ecommerce.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_review_votes",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_review_vote", columnNames = {"review_id", "customer_id"}),
        indexes = @Index(name = "idx_ec_reviewvote_rev", columnList = "review_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Builder.Default
    private LocalDateTime votedAt = LocalDateTime.now();

    public enum VoteType {HELPFUL, NOT_HELPFUL}
}
