// Path: com/asg/spindleserp/ecommerce/dto/EcTaxClassDTO.java
package com.asg.spindleserp.ecommerce.settings.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcTaxClassDTO {
    private Long   id;
    @NotBlank @Size(max = 30)
    private String classCode;
    @NotBlank @Size(max = 150)
    private String className;
    private String  description;
    private Boolean active;

    @Builder.Default
    private List<RuleDTO> rules = new ArrayList<>();

    private String createdAt;
    private String updatedAt;
    private String createdBy;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RuleDTO {
        private Long       id;
        private String     country;
        private String     division;
        private String     district;
        private String     taxName;
        private BigDecimal taxPercent;
        private LocalDate  effectiveFrom;
        private LocalDate  effectiveTo;
        private Boolean    active;
    }
}
