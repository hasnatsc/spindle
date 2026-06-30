package com.asg.spindleserp.ecommerce.marketing.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_notifications",
        indexes = {
                @Index(name = "idx_ec_notif_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_notif_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType notificationType;

    @Column(length = 300)
    private String title;
    @Column(columnDefinition = "text")
    private String message;
    @Builder.Default
    @Column(nullable = false)
    private boolean readFlag = false;
    private LocalDateTime sentAt;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum NotificationType {EMAIL, SMS, PUSH, IN_APP}
}
