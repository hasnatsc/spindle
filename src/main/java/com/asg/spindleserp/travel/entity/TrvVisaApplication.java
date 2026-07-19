package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // ── JPA object mappings ────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_service_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvBookingService bookingService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvPassenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visa_type_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvVisaType visaType;

    @Builder.Default
    @OneToMany(mappedBy = "visaApplication", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvVisaDocument> documents = new ArrayList<>();
}
