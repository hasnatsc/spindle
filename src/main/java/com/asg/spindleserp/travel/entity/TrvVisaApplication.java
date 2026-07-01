package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trv_visa_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvVisaApplication extends BaseEntity implements Serializable {

    public enum Status { PENDING, SUBMITTED, APPROVED, REJECTED, COLLECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", length = 100)
    private String applicationNumber;

    @Column(name = "submission_date")
    private LocalDate submissionDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Builder.Default
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    /** Soft FK → trv_booking_services (serviceType = VISA). */
    @Column(name = "booking_service_id", nullable = false)
    private Long bookingServiceId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "visa_type_id", nullable = false)
    private Long visaTypeId;
}
