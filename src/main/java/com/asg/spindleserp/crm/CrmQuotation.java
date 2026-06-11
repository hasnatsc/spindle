package com.asg.spindleserp.crm;

import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.production.order.Opportunity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_quotations",
        indexes = @Index(name = "idx_quot_opp", columnList = "opportunity_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmQuotation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_quotation_id")
    private BusinessDocument salesQuotation;

    @Column(name = "quoted_value", precision = 18, scale = 2)
    private BigDecimal quotedValue;
    @Column(name = "valid_until")
    private LocalDate validUntil;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";
    // DRAFT|SENT|ACCEPTED|REJECTED|REVISED
    @Builder.Default
    @Column(name = "revision_number")
    private Integer revisionNumber = 1;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
