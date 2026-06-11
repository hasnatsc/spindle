package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eco_coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_coup_store_code", columnNames = {"store_id", "code"}),
        indexes = @Index(name = "idx_coup_store", columnList = "store_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoCoupon extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // PERCENTAGE|FIXED_AMOUNT|FREE_SHIPPING|BUY_X_GET_Y
    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;
    @Builder.Default
    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;
    @Column(name = "usage_limit_total")
    private Integer usageLimitTotal;
    @Builder.Default
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser = 1;
    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;
    @Column(name = "valid_from")
    private LocalDate validFrom;
    @Column(name = "valid_to")
    private LocalDate validTo;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
