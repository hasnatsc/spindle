// Path: com/asg/spindleserp/ecommerce/dto/EcCustomerDTO.java
package com.asg.spindleserp.ecommerce.customerSupport.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCustomerDTO {
    private Long      id;

    @NotBlank @Size(max = 50)
    private String customerCode;
    @NotBlank @Size(max = 100)
    private String firstName;
    @Size(max = 100)
    private String lastName;
    @Size(max = 200)
    private String fullName;
    @Size(max = 200)
    private String email;
    @NotBlank @Size(max = 30)
    private String phone;

    private String    gender;           // MALE / FEMALE / OTHER
    private LocalDate dateOfBirth;
    private String    profileImage;

    // B2B
    private String    companyName;
    private String    taxNumber;
    private String    nationalId;

    // Verification
    private Boolean   emailVerified;
    private Boolean   phoneVerified;

    // Status
    private String    accountStatus;    // ACTIVE / BLOCKED / PENDING / DELETED
    private String    customerGroup;

    // Counters (read-only)
    private Integer   totalOrders;
    private BigDecimal totalPurchase;
    private Integer   rewardPoints;

    // ERP link (read-only display)
    private Long      erpSubAccountId;
    private String    erpSubAccountName;

    private Boolean   active;
    private String    lastLoginAt;
    private String    createdAt;
    private String    updatedAt;
    private String    createdBy;
    private String    updatedBy;
}
