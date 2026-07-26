package com.asg.spindleserp.travel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TrvPassengerDTO — standalone passenger CRUD payload for /travel/passengers.
 *
 * TrvBookingDTO.PassengerDTO stays exactly as-is for the nested
 * booking-save path; this DTO is the flat, self-contained version used by the
 * dedicated Passengers screen and by the "New Passenger" modal on the Air
 * Tickets page, so neither path disturbs the other.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPassengerDTO implements Serializable {

    private Long id;

    /** Owning booking — required on create, immutable afterwards. */
    private Long bookingId;
    /** Read-only display, e.g. "TRV-2026-0043". */
    private String bookingNo;

    // ── Identity ─────────────────────────────────────────────────────────────
    @Size(max = 10)
    private String title;

    @NotBlank(message = "First name is required.")
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    /** MALE | FEMALE | OTHER */
    private String gender;

    /** ADULT | CHILD | INFANT */
    @Builder.Default
    private String passengerType = "ADULT";

    @Builder.Default
    private Boolean isLeadPassenger = false;

    // ── Contact ──────────────────────────────────────────────────────────────
    @Size(max = 30)
    private String phone;

    @Email(message = "Enter a valid email address.")
    @Size(max = 150)
    private String email;

    @Size(max = 500)
    private String remarks;

    // ── Passport / travel document ───────────────────────────────────────────
    @Size(max = 50)
    private String passportNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportExpiry;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportIssueDate;

    @Size(max = 100)
    private String nationality;

    /** Alpha-3 issuing state from the MRZ, e.g. BGD. */
    @Size(max = 3)
    private String passportCountry;

    /** Issuing authority as printed, e.g. DIP/DHAKA. */
    @Size(max = 100)
    private String passportIssuingAuthority;

    /** National ID / personal number from the MRZ optional field. */
    @Size(max = 30)
    private String personalNumber;

    @Size(max = 120)
    private String placeOfBirth;

    // ── Personal-data page (used by the visa workflow) ───────────────────────
    @Size(max = 150)
    private String fatherName;

    @Size(max = 150)
    private String motherName;

    @Size(max = 500)
    private String permanentAddress;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 50)
    private String emergencyContactRelation;

    @Size(max = 30)
    private String emergencyContactPhone;

    // ── Scan audit trail ─────────────────────────────────────────────────────
    /** Raw MRZ kept verbatim so a bad auto-fill can always be re-derived. */
    @Size(max = 50)
    private String mrzLine1;
    @Size(max = 50)
    private String mrzLine2;
    @Size(max = 50)
    private String mrzLine3;

    /** trv_documents.id of the stored passport image, if one was uploaded. */
    private Long passportDocumentId;

    // ── Preference (one-to-one, saved in the same round trip) ────────────────
    private PreferenceDTO preference;

    // ── Read-only, list/view only ────────────────────────────────────────────
    private String fullName;
    private Integer age;
    /** Days until the passport expires; negative when already expired. */
    private Long daysToExpiry;
    /** Number of issued tickets — blocks delete when greater than zero. */
    private Integer ticketCount;
    private String createdBy;
    private String createdAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PreferenceDTO implements Serializable {
        private Long id;
        @Size(max = 100)  private String mealPreference;
        @Size(max = 100)  private String seatPreference;
        @Size(max = 300)  private String specialAssistance;
        @Size(max = 300)  private String dietaryRestriction;
        @Size(max = 500)  private String remarks;
    }
}
