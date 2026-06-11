package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_payment_methods",
        uniqueConstraints = @UniqueConstraint(name = "uk_pm_store_code", columnNames = {"store_id", "code"}),
        indexes = @Index(name = "idx_pm_store", columnList = "store_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoPaymentMethod extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType; // CASH_ON_DELIVERY|BKASH|NAGAD|ROCKET|BANK_TRANSFER|CARD|SSL_COMMERZ|STRIPE|PAYPAL|EMI
    @Column(length = 500)
    private String description;
    @Column(columnDefinition = "TEXT")
    private String instructions;
    @Column(name = "logo_url", length = 300)
    private String logoUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_config", columnDefinition = "jsonb")
    private Map<String, Object> gatewayConfig;
    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;
    @Column(name = "max_order_amount", precision = 18, scale = 2)
    private BigDecimal maxOrderAmount;
    @Column(name = "extra_charge_type", length = 20)
    private String extraChargeType; // FIXED|PERCENTAGE|NONE
    @Builder.Default
    @Column(name = "extra_charge_value", precision = 18, scale = 2)
    private BigDecimal extraChargeValue = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_test_mode", nullable = false)
    private Boolean isTestMode = false;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
