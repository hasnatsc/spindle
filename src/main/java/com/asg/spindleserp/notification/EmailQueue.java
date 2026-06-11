package com.asg.spindleserp.notification;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ntf_email_queue",
        indexes = @Index(name = "idx_email_status", columnList = "status,scheduled_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailQueue implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "to_email", nullable = false, length = 200)
    private String toEmail;
    @Column(name = "to_name", length = 200)
    private String toName;
    @Column(name = "cc_emails", columnDefinition = "TEXT")
    private String ccEmails;
    @Column(name = "bcc_emails", columnDefinition = "TEXT")
    private String bccEmails;
    @Column(nullable = false, length = 500)
    private String subject;
    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;
    @Column(name = "template_name", length = 100)
    private String templateName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_params", columnDefinition = "jsonb")
    private Map<String, Object> templateParams;

    @Column(name = "reference_type", length = 50)
    private String referenceType;
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING|SENT|FAILED|CANCELLED

    @Builder.Default
    @Column(nullable = false)
    private Integer attempts = 0;
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "scheduled_at")
    @Builder.Default
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
