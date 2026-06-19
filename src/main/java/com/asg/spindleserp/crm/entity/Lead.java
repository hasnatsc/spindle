package com.asg.spindleserp.crm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "crm_leads",
        uniqueConstraints = @UniqueConstraint(name = "uq_crl_org_no",
                columnNames = {"organization_id", "lead_no"}),
        indexes = {
                @Index(name = "idx_crl_org", columnList = "organization_id"),
                @Index(name = "idx_crl_status", columnList = "status"),
                @Index(name = "idx_crl_source", columnList = "source")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_to_id")
    private ChartOfAccountSub convertedTo;

    @Column(nullable = false, length = 50)
    private String leadNo;
    @Column(length = 200)
    private String companyName;
    @Column(nullable = false, length = 200)
    private String contactName;
    @Column(length = 100)
    private String contactEmail;
    @Column(length = 20)
    private String contactPhone;
    @Column(length = 100)
    private String designation;
    @Column(length = 100)
    private String country;
    @Column(length = 100)
    private String city;
    @Column(length = 50)
    private String source;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String leadType = "B2B";

    @Column(columnDefinition = "text")
    private String productInterest;
    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedQtyKg;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Lead.LeadStatus status = Lead.LeadStatus.NEW;

    @Column(columnDefinition = "text")
    private String remarks;

    public enum LeadStatus {NEW, CONTACTED, QUALIFIED, UNQUALIFIED, CONVERTED, LOST, DORMANT}
}
