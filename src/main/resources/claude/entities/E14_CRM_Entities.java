// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E14  CRM                                                  ║
// ║  Tables: crm_leads, crm_opportunities, crm_activities,                  ║
// ║           crm_contacts, crm_customer_feedback                           ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: crm/entity/Lead.java ───────────────────────────────────────────────
package com.hasnat.optimum.crm.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "crm_leads",
    uniqueConstraints = @UniqueConstraint(name = "uq_crl_org_no",
        columnNames = {"organization_id", "lead_no"}),
    indexes = {
        @Index(name = "idx_crl_org",    columnList = "organization_id"),
        @Index(name = "idx_crl_status", columnList = "status"),
        @Index(name = "idx_crl_source", columnList = "source")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_id") private User             assignedTo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "converted_to_id") private ChartOfAccountSub convertedTo;

    @Column(nullable = false, length = 50)  private String leadNo;
    @Column(length = 200) private String companyName;
    @Column(nullable = false, length = 200) private String contactName;
    @Column(length = 100) private String contactEmail;
    @Column(length = 20)  private String contactPhone;
    @Column(length = 100) private String designation;
    @Column(length = 100) private String country;
    @Column(length = 100) private String city;
    @Column(length = 50)  private String source;

    @Builder.Default @Column(nullable = false, length = 20) private String leadType = "B2B";

    @Column(columnDefinition = "text")     private String     productInterest;
    @Column(precision = 14, scale = 2)     private BigDecimal estimatedQtyKg;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStatus status = LeadStatus.NEW;

    @Column(columnDefinition = "text") private String remarks;

    public enum LeadStatus { NEW, CONTACTED, QUALIFIED, UNQUALIFIED, CONVERTED, LOST, DORMANT }
}


// ── FILE: crm/entity/Opportunity.java ────────────────────────────────────────
package com.hasnat.optimum.crm.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.security.entity.User;
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

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")    private ChartOfAccountSub customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id")        private Lead              lead;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_id") private User              assignedTo;

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


// ── FILE: crm/entity/CrmActivity.java ────────────────────────────────────────
package com.hasnat.optimum.crm.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "crm_activities",
    indexes = {
        @Index(name = "idx_cra_org",  columnList = "organization_id"),
        @Index(name = "idx_cra_opp",  columnList = "opportunity_id"),
        @Index(name = "idx_cra_date", columnList = "activity_date"),
        @Index(name = "idx_cra_user", columnList = "assigned_to_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrmActivity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "opportunity_id")  private Opportunity       opportunity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id")         private Lead              lead;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")     private ChartOfAccountSub customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_id")  private User              assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType activityType;

    @Column(nullable = false, length = 200) private String    subject;
    @Column(columnDefinition = "text")      private String    description;
    @Column(nullable = false)               private LocalDate activityDate;
    private Integer durationMinutes;
    @Column(length = 500) private String outcome;
    @Column(length = 500) private String nextAction;
    private LocalDate nextActionDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.PLANNED;

    public enum ActivityType {
        CALL, EMAIL, MEETING, VISIT, SAMPLE_SENT, QUOTATION, FOLLOW_UP, NOTE, OTHER
    }
    public enum ActivityStatus { PLANNED, COMPLETED, CANCELLED }
}


// ── FILE: crm/entity/Contact.java ────────────────────────────────────────────
package com.hasnat.optimum.crm.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_contacts",
    indexes = {
        @Index(name = "idx_crc_customer", columnList = "customer_id"),
        @Index(name = "idx_crc_org",      columnList = "organization_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")
    private ChartOfAccountSub customer;

    @Column(nullable = false, length = 100) private String firstName;
    @Column(length = 100) private String lastName;
    @Column(length = 100) private String designation;
    @Column(length = 100) private String department;
    @Column(length = 100) private String email;
    @Column(length = 20)  private String phone;
    @Column(length = 20)  private String mobile;
    @Column(length = 20)  private String whatsapp;

    @Builder.Default @Column(nullable = false) private boolean isPrimary = false;
    @Builder.Default @Column(nullable = false) private boolean isActive  = true;

    @Column(columnDefinition = "text") private String notes;
}


// ── FILE: crm/entity/CustomerFeedback.java ───────────────────────────────────
package com.hasnat.optimum.crm.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_customer_feedback",
    indexes = {
        @Index(name = "idx_crf_customer", columnList = "customer_id"),
        @Index(name = "idx_crf_status",   columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerFeedback extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private ChartOfAccountSub customer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "business_document_id")
    private BusinessDocument businessDocument;

    @Builder.Default @Column(nullable = false) private LocalDate feedbackDate  = LocalDate.now();
    @Builder.Default @Column(nullable = false, length = 30) private String feedbackType = "GENERAL";

    private Integer rating;  // 1-5
    @Column(length = 200) private String subject;
    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String resolution;
    @Column(length = 100) private String resolvedBy;
    private LocalDateTime resolvedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.OPEN;

    public enum FeedbackStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
}
