package com.asg.spindleserp.ecommerce.campaign;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_coupon",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_coupon",
                columnNames = {"organization_id", "coupon_code"}),
        indexes = {
                @Index(name = "idx_ec_coupon_code", columnList = "coupon_code"),
                @Index(name = "idx_ec_coupon_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String couponCode;

    @Column(length = 200)
    private String couponName;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DiscountType discountType;

    @Column(precision = 18, scale = 2)
    private BigDecimal discountValue;
    @Column(precision = 18, scale = 2)
    private BigDecimal minimumOrder;
    @Column(precision = 18, scale = 2)
    private BigDecimal maximumDiscount;

    private Integer usageLimit;

    @Builder.Default
    private Integer usagePerCustomer = 1;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public enum DiscountType {PERCENTAGE, FIXED}
}
