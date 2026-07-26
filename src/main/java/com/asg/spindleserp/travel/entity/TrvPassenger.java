package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TrvPassenger — a person travelling on a booking.
 *
 * DROP-IN REPLACEMENT. Everything that existed before is byte-for-byte
 * unchanged; the new block at the bottom adds the passport and personal-data
 * columns filled by the passport scanner. Requires migration
 * V330__trv_passenger_passport_fields.sql.
 */
@Entity
@Table(name = "trv_passengers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPassenger extends BaseEntity implements Serializable {

    public enum Gender { MALE, FEMALE, OTHER }

    public enum PassengerType { ADULT, CHILD, INFANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 10)
    private String title;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type", nullable = false, length = 10)
    private PassengerType passengerType = PassengerType.ADULT;

    @Builder.Default
    @Column(name = "is_lead_passenger", nullable = false)
    private Boolean isLeadPassenger = false;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private TrvBooking booking;

    // ═════════════════════════════════════════════════════════════════════════
    // PASSPORT / TRAVEL DOCUMENT  — added for the passport scanner
    // ═════════════════════════════════════════════════════════════════════════

    /** Date of issue as printed on the data page. */
    @Column(name = "passport_issue_date")
    private LocalDate passportIssueDate;

    /** Alpha-3 issuing state straight from the MRZ, e.g. BGD. */
    @Column(name = "passport_country", length = 3)
    private String passportCountry;

    /** Issuing authority as printed, e.g. DIP/DHAKA. Not in the MRZ — typed or OCR'd from the VIZ. */
    @Column(name = "passport_issuing_authority", length = 100)
    private String passportIssuingAuthority;

    /** National ID / personal number carried in the MRZ optional data field. */
    @Column(name = "personal_number", length = 30)
    private String personalNumber;

    @Column(name = "place_of_birth", length = 120)
    private String placeOfBirth;

    // ═════════════════════════════════════════════════════════════════════════
    // PERSONAL DATA PAGE — required by most visa applications
    // ═════════════════════════════════════════════════════════════════════════

    @Column(name = "father_name", length = 150)
    private String fatherName;

    @Column(name = "mother_name", length = 150)
    private String motherName;

    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    // ═════════════════════════════════════════════════════════════════════════
    // SCAN AUDIT TRAIL
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verbatim MRZ lines. Storing them means an auto-fill mistake can always
     * be traced and re-derived without asking the customer for the passport
     * a second time. Kept as plain varchar — an MRZ line is never over 44
     * characters, so there is no reason to reach for @Lob here.
     */
    @Column(name = "mrz_line1", length = 50)
    private String mrzLine1;

    @Column(name = "mrz_line2", length = 50)
    private String mrzLine2;

    @Column(name = "mrz_line3", length = 50)
    private String mrzLine3;

    /**
     * Soft reference to trv_documents.id holding the uploaded passport image.
     * Deliberately NOT a JPA relationship — trv_documents is polymorphic
     * (entityType + entityId), exactly like trv_booking_services.referenceId.
     */
    @Column(name = "passport_document_id")
    private Long passportDocumentId;

    // ── Child collections ─────────────────────────────────────────────────

    @Builder.Default
    @OneToMany(mappedBy = "passenger", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvPassengerTicket> tickets = new ArrayList<>();

    @OneToOne(mappedBy = "passenger", fetch = FetchType.LAZY)
    @JsonIgnore
    private TrvPassengerPreference preference;

    // ── Convenience ───────────────────────────────────────────────────────

    @Transient
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) sb.append(title).append(' ');
        sb.append(firstName == null ? "" : firstName);
        if (lastName != null && !lastName.isBlank()) sb.append(' ').append(lastName);
        return sb.toString().trim();
    }
}
