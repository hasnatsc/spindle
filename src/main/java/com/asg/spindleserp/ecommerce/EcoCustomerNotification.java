package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_customer_notifications",
        indexes = {
                @Index(name = "idx_ecnotif_cust", columnList = "customer_id"),
                @Index(name = "idx_ecnotif_unsent", columnList = "is_sent,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoCustomerNotification implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eco_order_id")
    private EcoOrder ecoOrder;
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    // ORDER_CONFIRMED|PAYMENT_RECEIVED|ORDER_PACKED|ORDER_SHIPPED|OUT_FOR_DELIVERY|
    // DELIVERED|REFUND_PROCESSED|RETURN_APPROVED|CART_ABANDONMENT|PRICE_DROP|BACK_IN_STOCK
    @Column(nullable = false, length = 20)
    private String channel; // EMAIL|SMS|PUSH|WHATSAPP
    @Column(length = 300)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(name = "template_name", length = 100)
    private String templateName;
    @Builder.Default
    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
