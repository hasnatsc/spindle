package com.asg.spindleserp.ecommerce.settings;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_scheduled_jobs",
        indexes = @Index(name = "idx_ec_job_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // organizationId stored as Long (not BaseEntity — system-level jobs may be org-agnostic)
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(length = 200)
    private String jobName;
    @Column(length = 100)
    private String cronExpression;
    private LocalDateTime lastRun;
    private LocalDateTime nextRun;

    @Builder.Default
    @Column(length = 20)
    private String lastResult = "PENDING";  // SUCCESS | FAILED | RUNNING

    @Column(columnDefinition = "text")
    private String errorMessage;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
