package com.asg.spindleserp.crm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_customer_feedback",
        indexes = {
                @Index(name = "idx_crf_customer", columnList = "customer_id"),
                @Index(name = "idx_crf_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private ChartOfAccountSub customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_document_id")
    private BusinessDocument businessDocument;

    @Builder.Default
    @Column(nullable = false)
    private LocalDate feedbackDate = LocalDate.now();
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String feedbackType = "GENERAL";

    private Integer rating;  // 1-5
    @Column(length = 200)
    private String subject;
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String resolution;
    @Column(length = 100)
    private String resolvedBy;
    private LocalDateTime resolvedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerFeedback.FeedbackStatus status = CustomerFeedback.FeedbackStatus.OPEN;

    public enum FeedbackStatus {OPEN, IN_PROGRESS, RESOLVED, CLOSED}
}
