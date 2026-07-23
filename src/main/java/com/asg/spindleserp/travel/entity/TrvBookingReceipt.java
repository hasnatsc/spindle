package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trv_booking_receipts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvBookingReceipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private TrvBooking booking;

    @Column(name = "payment_mode", nullable = false, length = 20)
    private String paymentMode;  // CASH | BANK | BKASH | NAGAD | CARD

    @Column(name = "sub_account_id")
    private Long subAccountId;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
}
