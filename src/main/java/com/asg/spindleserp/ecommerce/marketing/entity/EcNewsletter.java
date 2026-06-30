package com.asg.spindleserp.ecommerce.marketing.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_newsletter",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_newsletter",
                columnNames = {"organization_id", "email"}),
        indexes = {
                @Index(name = "idx_ec_newsletter_status", columnList = "subscription_status"),
                @Index(name = "idx_ec_newsletter_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcNewsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String email;
    @Column(length = 200)
    private String fullName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.SUBSCRIBED;

    @Column(length = 255)
    private String verificationToken;
    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;
    @Builder.Default
    private LocalDateTime subscribedAt = LocalDateTime.now();
    private LocalDateTime unsubscribedAt;

    public enum SubscriptionStatus {SUBSCRIBED, UNSUBSCRIBED, BOUNCED}
}
