package com.asg.spindleserp.ecommerce.customerSupport;

import com.asg.spindleserp.ecommerce.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_customer_wallet",
        indexes = {
                @Index(name = "idx_ec_wallet_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_wallet_ref", columnList = "reference_type,reference_id"),
                @Index(name = "idx_ec_wallet_time", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCustomerWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType transactionType;

    // 'ORDER' | 'REFUND' | 'TOPUP' | 'ADJUSTMENT' | 'REWARD_REDEEM'
    @Column(length = 50)
    private String referenceType;
    private Long referenceId;

    @Column(length = 500)
    private String narration;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(length = 100)
    private String createdBy;

    public enum TransactionType {CREDIT, DEBIT}
}
