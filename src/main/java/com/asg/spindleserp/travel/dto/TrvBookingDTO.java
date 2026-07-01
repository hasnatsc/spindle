package com.asg.spindleserp.travel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvBookingDTO {

    private Long id;

    private String bookingNo; // server-generated on create via stp_document_sequences pattern

    @NotBlank(message = "Booking type is required")
    private String bookingType; // PACKAGE | HOTEL | AIR | COMBINED

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    private LocalDate travelStartDate;
    private LocalDate travelEndDate;

    @Builder.Default
    private String status = "DRAFT";

    @Builder.Default
    private String currency = "BDT";

    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;

    @Size(max = 1000)
    private String remarks;

    // ── AJAX Select2 refs ──────────────────────────────────────────────────
    private Long   partyId;         private String partyDisplay;
    private Long   leadId;          private String leadDisplay;
    private Long   opportunityId;   private String opportunityDisplay;
    private Long   salesAgentId;    private String salesAgentDisplay;

    private Long   approvalRequestId;
    private Long   journalEntryId;

    @Builder.Default
    @Valid
    private List<ServiceLineDTO> services = new ArrayList<>();

    @Builder.Default
    @Valid
    private List<PassengerDTO> passengers = new ArrayList<>();

    private String createdAt;
    private String updatedAt;
    private String createdBy;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ServiceLineDTO {
        private Long id;
        @NotBlank private String serviceType; // HOTEL | AIR | PACKAGE
        private Long referenceId;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
        private Long costCenterId; private String costCenterDisplay;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PassengerDTO {
        private Long id;
        private String title;
        @NotBlank private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String gender;
        private String passportNumber;
        private LocalDate passportExpiry;
        private String nationality;
        @Builder.Default private String passengerType = "ADULT";
        @Builder.Default private Boolean isLeadPassenger = false;
        private String phone;
        private String email;
        private String remarks;
        private PreferenceDTO preference;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PreferenceDTO {
        private Long id;
        private String mealPreference;
        private String seatPreference;
        private String specialAssistance;
        private String dietaryRestriction;
        private String remarks;
    }
}
