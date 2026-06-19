package com.asg.spindleserp.crm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "crm_activities",
        indexes = {
                @Index(name = "idx_cra_org", columnList = "organization_id"),
                @Index(name = "idx_cra_opp", columnList = "opportunity_id"),
                @Index(name = "idx_cra_date", columnList = "activity_date"),
                @Index(name = "idx_cra_user", columnList = "assigned_to_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private ChartOfAccountSub customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CrmActivity.ActivityType activityType;

    @Column(nullable = false, length = 200)
    private String subject;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private LocalDate activityDate;
    private Integer durationMinutes;
    @Column(length = 500)
    private String outcome;
    @Column(length = 500)
    private String nextAction;
    private LocalDate nextActionDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrmActivity.ActivityStatus status = CrmActivity.ActivityStatus.PLANNED;

    public enum ActivityType {
        CALL, EMAIL, MEETING, VISIT, SAMPLE_SENT, QUOTATION, FOLLOW_UP, NOTE, OTHER
    }

    public enum ActivityStatus {PLANNED, COMPLETED, CANCELLED}
}
