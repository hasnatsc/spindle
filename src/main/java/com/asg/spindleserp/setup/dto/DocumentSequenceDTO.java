package com.asg.spindleserp.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentSequenceDTO {

    private Long id;

    @NotNull(message = "Organisation is required")
    private Long organizationId;

    @NotBlank(message = "Prefix is required")
    @Size(max = 20, message = "Prefix must not exceed 20 characters")
    private String prefix;

    @NotBlank(message = "Year code is required")
    @Size(max = 7, message = "Year code must not exceed 7 characters")
    private String yearCode;

    @Builder.Default
    private Integer lastSeq = 0;

    private String createdAt;
    private String updatedAt;
}
