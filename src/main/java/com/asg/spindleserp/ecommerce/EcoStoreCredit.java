package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eco_store_credits",
        indexes = @Index(name = "idx_credit_cust", columnList = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoStoreCredit extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // CREDIT|DEBIT|EXPIRE
    @Column(length = 200)
    private String reason;
    @Column(name = "expires_at")
    private LocalDate expiresAt;
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    @Column(name = "reference_id")
    private Long referenceId;
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
