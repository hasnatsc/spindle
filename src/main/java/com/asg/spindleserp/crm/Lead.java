package com.asg.spindleserp.crm;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.locations.Country;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_leads",
        indexes = {
                @Index(name = "idx_lead_org", columnList = "organization_id"),
                @Index(name = "idx_lead_status", columnList = "status"),
                @Index(name = "idx_lead_assigned", columnList = "assigned_to_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    @Column(length = 200)
    private String company;
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    @Column(length = 100)
    private String email;
    @Column(length = 20)
    private String phone;
    @Column(length = 20)
    private String mobile;
    @Column(length = 200)
    private String website;
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String source = "UNKNOWN";
    // WEBSITE|REFERRAL|TRADE_SHOW|COLD_CALL|SOCIAL_MEDIA|EMAIL_CAMPAIGN|WALK_IN|OTHER

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "NEW";
    // NEW|CONTACTED|QUALIFIED|DISQUALIFIED|CONVERTED

    @Builder.Default
    @Column(name = "lead_score")
    private Integer leadScore = 0;
    @Column(name = "estimated_value", precision = 18, scale = 2)
    private BigDecimal estimatedValue;
    @Column(length = 100)
    private String industry;
    @Column(name = "product_interest", columnDefinition = "TEXT")
    private String productInterest;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "disqualify_reason", columnDefinition = "TEXT")
    private String disqualifyReason;
    @Column(name = "converted_at")
    private LocalDateTime convertedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
