package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_order_status_history",
        indexes = @Index(name = "idx_osh_order", columnList = "eco_order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoOrderStatusHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @Column(name = "from_status", length = 30)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;
    @Column(columnDefinition = "TEXT")
    private String comment;
    @Builder.Default
    @Column(name = "notify_customer", nullable = false)
    private Boolean notifyCustomer = false;
    @Column(name = "changed_by", length = 100)
    private String changedBy;
    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
