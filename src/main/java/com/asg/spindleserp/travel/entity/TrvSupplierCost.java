package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "trv_supplier_costs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvSupplierCost extends BaseEntity implements Serializable {

    public enum PaymentStatus { UNPAID, PARTIAL, PAID }

    @Builder.Default
    @Column(name = "cost_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    /** Soft FK → trv_booking_services. */
    @Column(name = "booking_service_id", nullable = false)
    private Long bookingServiceId;

    /** Soft FK → acc_chart_of_accounts_sub (SUPPLIER sub-type). */
    @Column(name = "supplier_id")
    private Long supplierId;
}
