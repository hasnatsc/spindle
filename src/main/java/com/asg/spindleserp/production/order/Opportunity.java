package com.asg.spindleserp.production.order;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.crm.Lead;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crm_opportunities",
        indexes = {
                @Index(name = "idx_opp_org", columnList = "organization_id"),
                @Index(name = "idx_opp_stage", columnList = "stage"),
                @Index(name = "idx_opp_customer", columnList = "customer_id"),
                @Index(name = "idx_opp_close", columnList = "expected_close_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opportunity extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private SubAccount customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_sq_id")
    private BusinessDocument convertedSq;

    @Column(nullable = false, length = 300)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String stage = "PROSPECT";
    // PROSPECT|PROPOSAL|NEGOTIATION|WON|LOST|ON_HOLD

    @Column(name = "estimated_value", precision = 18, scale = 2)
    private BigDecimal estimatedValue;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";
    @Column(precision = 5, scale = 2)
    private BigDecimal probability;
    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;
    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;
    @Column(name = "loss_reason", columnDefinition = "TEXT")
    private String lossReason;
    @Column(length = 200)
    private String competitor;
    @Column(length = 50)
    private String source;
    @Column(name = "product_interest", columnDefinition = "TEXT")
    private String productInterest;
    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;
    @Column(name = "next_action_date")
    private LocalDate nextActionDate;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
