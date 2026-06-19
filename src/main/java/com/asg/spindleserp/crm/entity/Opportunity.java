package com.asg.spindleserp.crm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crm_opportunities",
    uniqueConstraints = @UniqueConstraint(name = "uq_cro_org_no",
        columnNames = {"organization_id", "opportunity_no"}),
    indexes = {
        @Index(name = "idx_cro_org",      columnList = "organization_id"),
        @Index(name = "idx_cro_stage",    columnList = "stage"),
        @Index(name = "idx_cro_customer", columnList = "customer_id"),
        @Index(name = "idx_cro_lead",     columnList = "lead_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Opportunity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")    private ChartOfAccountSub customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id")        private Lead lead;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_id") private User assignedTo;

    @Column(nullable = false, length = 50)  private String opportunityNo;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "text")      private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OpportunityStage stage = OpportunityStage.PROSPECT;

    @Builder.Default @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal probability = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2) private BigDecimal estimatedValue;
    @Builder.Default @Column(nullable = false, length = 3) private String currency = "BDT";

    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    @Column(length = 500) private String lostReason;
    @Column(columnDefinition = "text") private String remarks;

    public enum OpportunityStage { PROSPECT, QUALIFIED, PROPOSAL, NEGOTIATION, WON, LOST }
}