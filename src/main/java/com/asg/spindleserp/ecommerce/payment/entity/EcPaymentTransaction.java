package com.asg.spindleserp.ecommerce.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_payment_transactions",
        indexes = {
                @Index(name = "idx_ec_pmttx_payment", columnList = "payment_id"),
                @Index(name = "idx_ec_pmttx_time", columnList = "transaction_time")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private EcPayment payment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType transactionType;

    @Column(length = 50)
    private String gatewayName;

    // JSONB — use hypersistence JsonBinaryType (same approach as other JSONB columns)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String responsePayload;

    @Column(length = 50)
    private String gatewayStatus;

    @Column(length = 200)
    private String gatewayReference;

    @Builder.Default
    private LocalDateTime transactionTime = LocalDateTime.now();

    public enum TransactionType {AUTHORIZE, CAPTURE, REFUND, VOID}
}
