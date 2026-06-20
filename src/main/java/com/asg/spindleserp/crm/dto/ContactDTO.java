package com.asg.spindleserp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactDTO {
    private Long   id;

    // Customer — AJAX Select2 → /accounts/sub-accounts/search?subAccountType=CUSTOMER
    private Long   customerId;
    private String customerDisplay;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Size(max = 100) private String lastName;
    @Size(max = 100) private String designation;
    @Size(max = 100) private String department;
    @Size(max = 100) private String email;
    @Size(max = 20)  private String phone;
    @Size(max = 20)  private String mobile;
    @Size(max = 20)  private String whatsapp;

    @Builder.Default private Boolean primary = false;
    @Builder.Default private Boolean active  = true;

    private String notes;
    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
