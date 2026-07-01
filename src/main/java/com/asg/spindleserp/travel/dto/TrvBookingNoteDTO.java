package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvBookingNoteDTO {

    private Long id;

    @NotBlank
    private String noteText;

    @Builder.Default
    private String noteType = "INTERNAL";

    private Long bookingId;
    private String createdBy;
    private String createdAt;
}
