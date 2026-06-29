package com.asg.spindleserp.ecommerce.marketing;

import com.asg.spindleserp.ecommerce.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_referrals",
        indexes = @Index(name = "idx_ec_referral_aff", columnList = "affiliate_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliate_id")
    private EcAffiliate affiliate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_customer")
    private EcCustomer referredCustomer;

    @Column(precision = 18, scale = 2)
    private BigDecimal referralBonus;
    @Builder.Default
    private LocalDateTime referralDate = LocalDateTime.now();
}
