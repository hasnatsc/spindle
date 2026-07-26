package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * TrvPaymentModeAccount — maps a payment mode (CASH/BANK/BKASH/NAGAD/CARD)
 * to a default sub-account (GL head) per organization.
 *
 * Used by receipt processing to auto-populate the DR account when
 * creating RECEIPT_VOUCHER journal entries.
 */
@Entity
@Table(name = "trv_payment_mode_accounts", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_paymode_org", columnNames = {"organization_id", "payment_mode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPaymentModeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private TrvBookingReceipt.PaymentMode paymentMode;

    /** FK → acc_chart_of_accounts_sub.id (the default GL head for this payment mode) */
    @Column(name = "sub_account_id")
    private Long subAccountId;
}
