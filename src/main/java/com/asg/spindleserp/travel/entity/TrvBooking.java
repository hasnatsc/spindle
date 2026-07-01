package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TrvBooking — header. Mirrors the global_business_documents shape conceptually
 * (status / party / approval / GL bridge) even though travel bookings are a
 * separate table family, since a travel booking is passenger-centric rather
 * than line-item-centric.
 */
@Entity
@Table(name = "trv_bookings", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_booking_org_no", columnNames = {"organization_id", "booking_no"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvBooking extends BaseEntity implements Serializable {

    public enum BookingType { PACKAGE, HOTEL, AIR, COMBINED }

    public enum Status { DRAFT, CONFIRMED, PARTIALLY_PAID, PAID, CANCELLED, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_no", nullable = false, length = 50)
    private String bookingNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false, length = 20)
    private BookingType bookingType;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "travel_start_date")
    private LocalDate travelStartDate;

    @Column(name = "travel_end_date")
    private LocalDate travelEndDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Builder.Default
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "due_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    /** organizationId is EAGER-safe here because it's a plain Long, not a lazy proxy. */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** Soft FK → acc_chart_of_accounts_sub (CUSTOMER sub-type). */
    @Column(name = "party_id")
    private Long partyId;

    /** Soft FK → crm_leads. */
    @Column(name = "lead_id")
    private Long leadId;

    /** Soft FK → crm_opportunities. */
    @Column(name = "opportunity_id")
    private Long opportunityId;

    /** Soft FK → sec_users. */
    @Column(name = "sales_agent_id")
    private Long salesAgentId;

    /** Soft FK → apr_requests. */
    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    /** Soft FK → acc_journal_entry_master, set once the booking is GL-posted. */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TrvBookingService> services = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TrvPassenger> passengers = new ArrayList<>();
}
