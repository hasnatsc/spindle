package com.asg.spindleserp.ecommerce.review;

import com.asg.spindleserp.ecommerce.EcCustomer;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_product_answers",
        indexes = @Index(name = "idx_ec_answer_q", columnList = "question_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcProductAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(nullable = false, columnDefinition = "text")
    private String answerText;
    @Builder.Default
    @Column(nullable = false)
    private boolean officialAnswer = false;
    @Builder.Default
    @Column(nullable = false)
    private Integer helpfulCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerStatus answerStatus = AnswerStatus.APPROVED;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AnswerStatus {PENDING, APPROVED, REJECTED}
}
