package com.asg.spindleserp.travel.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * PassportOcrResult — parsed fields extracted from a passport image via OCR.
 * All fields are populated on a best-effort basis; callers should check for null.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PassportOcrResult {

    /** Given / first name(s) as printed on the passport. */
    private String firstName;

    /** Surname / last name. */
    private String lastName;

    /** Date of birth parsed from the passport MRZ or visual zone. */
    private LocalDate dateOfBirth;

    /** Passport number (alphanumeric). */
    private String passportNumber;

    /** Nationality (3-letter ICAO code, e.g. "BGD", "USA", or full name). */
    private String nationality;

    /** Passport expiry date. */
    private LocalDate passportExpiry;

    /** Full raw text extracted by the OCR engine (for debugging / manual override). */
    private String rawOcrText;

    /** MRZ line(s) if detected and decoded (for debugging). */
    private String mrzText;

    /** Whether this result was parsed from the MRZ zone (more reliable). */
    @Builder.Default
    private boolean fromMrz = false;
}
