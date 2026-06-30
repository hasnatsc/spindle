package com.asg.spindleserp.ecommerce.marketing.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_affiliates",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_affiliate",
                columnNames = {"organization_id", "affiliate_code"}),
        indexes = {
                @Index(name = "idx_ec_affiliate_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_affiliate_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcAffiliate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(length = 100)
    private String affiliateCode;
    @Column(precision = 8, scale = 2)
    private BigDecimal commissionRate;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal totalCommission = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliateStatus status = AffiliateStatus.ACTIVE;

    public enum AffiliateStatus {ACTIVE, BLOCKED, SUSPENDED}
}
