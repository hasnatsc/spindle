package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvTourGuideDTO {

    private Long id;

    @NotBlank private String guideName;
    private String phone;
    private String email;
    private String languages;
    @Builder.Default private Boolean isActive = true;
}
