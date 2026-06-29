package com.asg.spindleserp.ecommerce.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_order_status_history",
        indexes = {
                @Index(name = "idx_ec_orderhist_order", columnList = "order_id"),
                @Index(name = "idx_ec_orderhist_time", columnList = "changed_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "text")
    private String remarks;
    @Column(length = 100)
    private String changedBy;
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
    @Column(length = 50)
    private String ipAddress;
}
