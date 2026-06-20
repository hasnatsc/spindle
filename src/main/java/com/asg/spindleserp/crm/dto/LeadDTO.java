package com.asg.spindleserp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadDTO {
    private Long   id;
    private String leadNo;             // auto-generated

    // Assigned user — AJAX Select2 → /users/search
    private Long   assignedToId;
    private String assignedToDisplay;

    // Converted customer (optional)
    private Long   convertedToId;
    private String convertedToDisplay;

    @Size(max = 200) private String companyName;

    @NotBlank(message = "Contact name is required")
    @Size(max = 200)
    private String contactName;

    @Size(max = 100) private String contactEmail;
    @Size(max = 20)  private String contactPhone;
    @Size(max = 100) private String designation;
    @Size(max = 100) private String country;
    @Size(max = 100) private String city;
    @Size(max = 50)  private String source;

    /** B2B | B2C | EXPORT | DOMESTIC */
    @Builder.Default private String leadType = "B2B";

    private String     productInterest;
    private BigDecimal estimatedQtyKg;

    /** NEW | CONTACTED | QUALIFIED | UNQUALIFIED | CONVERTED | LOST | DORMANT */
    @Builder.Default private String status = "NEW";

    private String remarks;
    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
