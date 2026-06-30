package com.asg.spindleserp.ecommerce.marketing.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ec_gift_cards",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_gift_card",
                columnNames = {"organization_id", "gift_card_code"}),
        indexes = {
                @Index(name = "idx_ec_giftcard_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_giftcard_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcGiftCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String giftCardCode;
    @Column(precision = 18, scale = 2)
    private BigDecimal initialAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal balanceAmount;
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
