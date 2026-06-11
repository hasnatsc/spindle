package com.asg.spindleserp.crm;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.production.order.Opportunity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_activities",
        indexes = {
                @Index(name = "idx_act_opp", columnList = "opportunity_id"),
                @Index(name = "idx_act_type", columnList = "activity_type"),
                @Index(name = "idx_act_status", columnList = "status"),
                @Index(name = "idx_act_due", columnList = "due_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends BaseOrgEntity {

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
    @JoinColumn(name = "contact_id")
    private Contact contact;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;
    // CALL|EMAIL|MEETING|DEMO|FOLLOW_UP|TASK|NOTE|SITE_VISIT|PRESENTATION

    @Column(nullable = false, length = 300)
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(columnDefinition = "TEXT")
    private String outcome;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN"; // OPEN|COMPLETED|CANCELLED|OVERDUE

    // Call-specific
    @Column(name = "call_duration_mins")
    private Integer callDurationMins;
    @Column(name = "call_direction", length = 10)
    private String callDirection;

    // Meeting-specific
    @Column(length = 300)
    private String location;
    @Column(columnDefinition = "TEXT")
    private String attendees;
    @Column(name = "meeting_notes", columnDefinition = "TEXT")
    private String meetingNotes;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
    @Builder.Default
    @Column(name = "follow_up_required")
    private Boolean followUpRequired = false;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
