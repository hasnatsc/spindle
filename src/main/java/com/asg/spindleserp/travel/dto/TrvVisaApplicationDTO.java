package com.asg.spindleserp.travel.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvVisaApplicationDTO {

    private Long id;

    private String applicationNumber;
    private LocalDate submissionDate;
    private LocalDate expectedDate;
    private LocalDate approvalDate;
    @Builder.Default private String status = "PENDING";
    @Builder.Default private BigDecimal feeAmount = BigDecimal.ZERO;
    private String remarks;

    private Long bookingServiceId;
    private Long passengerId; private String passengerName;
    private Long visaTypeId; private String visaTypeDisplay;

    @Builder.Default
    @Valid
    private List<DocumentDTO> documents = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocumentDTO {
        private Long id;
        private String documentName;
        @Builder.Default private Boolean isReceived = false;
        private String remarks;
    }
}
