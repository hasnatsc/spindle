package com.asg.spindleserp.travel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * PassportScanDTO — everything the MRZ gives us, plus the derived values the
 * New / Edit Passenger form actually needs.
 *
 * Field naming deliberately mirrors TrvPassengerDTO where they overlap so the
 * browser can map scan result to form field with no translation table.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PassportScanDTO implements Serializable {

    /** False only when nothing usable could be read; message explains why. */
    @Builder.Default
    private boolean success = false;

    /** Operator-facing summary — safe to show verbatim in a toast. */
    private String message;

    /** TD1 | TD2 | TD3 */
    private String format;

    // ── Raw MRZ, kept so the operator can re-parse or audit later ────────────
    private String mrzLine1;
    private String mrzLine2;
    private String mrzLine3;

    // ── Straight off the MRZ ─────────────────────────────────────────────────
    /** "P" for a passport booklet, "PD"/"PS" for diplomatic/service, "I" for ID. */
    private String documentCode;

    /** Alpha-3 of the issuing state, e.g. BGD. */
    private String issuingCountry;
    private String issuingCountryName;

    /** Given names — maps to the form's First Name. */
    private String givenNames;

    /** Family name — maps to the form's Last Name. */
    private String surname;

    private String passportNumber;

    /** Alpha-3 nationality, e.g. BGD. */
    private String nationality;
    /** Demonym for the form's Nationality box, e.g. Bangladeshi. */
    private String nationalityName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportExpiry;

    /** M | F | X exactly as printed. */
    private String sex;

    /** MALE | FEMALE | OTHER — matches TrvPassenger.Gender. */
    private String gender;

    /** National ID / personal number carried in the optional MRZ field. */
    private String personalNumber;

    // ── Printed-zone (VIZ) fields — best-effort OCR of the visible page ──────
    // These are NOT in the MRZ and carry no check digits: they are harvested by
    // VizExtractor from a plain OCR pass over the printed page, so treat them
    // as pre-fill suggestions, never as verified data.
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportIssueDate;

    /** e.g. DIP/DHAKA. */
    private String passportIssuingAuthority;

    private String placeOfBirth;
    private String fatherName;
    private String motherName;
    private String permanentAddress;
    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactPhone;

    /**
     * True when the MRZ name split had to be repaired or failed — tells
     * VizExtractor it may override the names from the printed zone.
     */
    @Builder.Default
    private boolean namesUncertain = false;

    // ── Derived conveniences ─────────────────────────────────────────────────
    private Integer age;

    /** Mr | Ms — a courtesy guess from sex; always editable. */
    private String suggestedTitle;

    /** ADULT | CHILD | INFANT from IATA age bands at scan time. */
    @Builder.Default
    private String suggestedPassengerType = "ADULT";

    /** False when any ICAO check digit failed — the UI highlights the form. */
    @Builder.Default
    private boolean checkDigitsValid = true;

    /** Human-readable notes: failed check digits, expiry inside six months, etc. */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /** Where the text came from: MRZ_MANUAL | OCR_CLIENT | OCR_SERVER. */
    private String source;
}
