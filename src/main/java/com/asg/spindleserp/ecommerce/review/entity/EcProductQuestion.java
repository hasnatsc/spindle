package com.asg.spindleserp.ecommerce.review.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ec_product_questions",
        indexes = {
                @Index(name = "idx_ec_qa_prod", columnList = "product_id"),
                @Index(name = "idx_ec_qa_status", columnList = "question_status"),
                @Index(name = "idx_ec_qa_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcProductQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(length = 250)
    private String questionTitle;
    @Column(nullable = false, columnDefinition = "text")
    private String questionText;
    @Builder.Default
    @Column(nullable = false)
    private boolean anonymous = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus questionStatus = QuestionStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Integer views = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcProductAnswer> answers = new ArrayList<>();

    public enum QuestionStatus {PENDING, APPROVED, REJECTED, ANSWERED}
}
