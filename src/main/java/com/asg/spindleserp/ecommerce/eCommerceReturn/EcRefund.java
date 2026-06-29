package com.asg.spindleserp.ecommerce.eCommerceReturn;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_refunds",
        indexes = {
                @Index(name = "idx_ec_refund_return", columnList = "return_id"),
                @Index(name = "idx_ec_refund_gl", columnList = "journal_entry_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private EcReturn ecReturn;

    @Column(length = 50)
    private String refundNo;

    @Builder.Default
    private LocalDateTime refundDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RefundMethod refundMethod;

    @Column(precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 150)
    private String transactionReference;

    @Column(columnDefinition = "text")
    private String remarks;

    // GL bridge: PAYMENT_VOUCHER (JournalEntryMaster)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    public enum RefundMethod {BKASH, NAGAD, ROCKET, CARD, BANK, WALLET, CASH}
}
