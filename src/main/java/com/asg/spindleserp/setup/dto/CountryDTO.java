package com.asg.spindleserp.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CountryDTO {

    private Long id;

    @NotNull(message = "Currency is required")
    private Long currencyId;

    private String currencyCode;
    private String currencyName;

    @NotBlank(message = "ISO Code (3) is required")
    @Size(max = 3, message = "ISO Code must not exceed 3 characters")
    private String isoCode;

    @NotBlank(message = "ISO Code (2) is required")
    @Size(max = 2, message = "ISO Code2 must not exceed 2 characters")
    private String isoCode2;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 150) private String nameNative;
    @Size(max = 10)  private String phoneCode;

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
}
