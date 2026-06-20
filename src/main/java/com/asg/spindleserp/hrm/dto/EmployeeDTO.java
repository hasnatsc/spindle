package com.asg.spindleserp.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeDTO {
    private Long   id;
    private String employeeCode;  // auto-generated

    // Department — AJAX Select2
    @NotNull(message = "Department is required")
    private Long   departmentId;
    private String departmentDisplay;

    // Designation — AJAX Select2
    @NotNull(message = "Designation is required")
    private Long   designationId;
    private String designationDisplay;

    // Reporting Manager — AJAX Select2 (optional)
    private Long   reportingManagerId;
    private String reportingManagerDisplay;

    // Linked user (optional)
    private Long   userId;
    private String userDisplay;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Size(max = 100) private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 20)
    private String phone;

    /** MALE | FEMALE | OTHER */
    @Builder.Default private String gender = "MALE";

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @Size(max = 10)  private String bloodGroup;
    @Size(max = 20)  private String maritalStatus;
    @Size(max = 50)  private String nationalId;
    @Size(max = 50)  private String passportNumber;

    /** PERMANENT | CONTRACT | TEMPORARY | INTERN | PART_TIME | CONSULTANT */
    @Builder.Default private String employeeType = "PERMANENT";

    /** ACTIVE | INACTIVE | ON_LEAVE | SUSPENDED | TERMINATED | RESIGNED | RETIRED */
    @Builder.Default private String status = "ACTIVE";

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate resignationDate;
    private LocalDate exitDate;

    // Salary snapshot (from Employee entity — quick view)
    private BigDecimal basicSalary;
    private BigDecimal grossSalary;

    @Size(max = 50)  private String bankName;
    @Size(max = 50)  private String bankAccountNumber;
    @Size(max = 50)  private String bankBranch;
    @Size(max = 50)  private String workLocation;
    @Size(max = 50)  private String workShift;

    @Builder.Default private Integer annualLeaveDays  = 0;
    @Builder.Default private Integer sickLeaveDays    = 0;
    @Builder.Default private Integer casualLeaveDays  = 0;

    @Size(max = 100) private String emergencyContactName;
    @Size(max = 20)  private String emergencyContactPhone;
    @Size(max = 100) private String emergencyContactRelation;
    @Size(max = 255) private String profilePicture;
    @Size(max = 1000)private String notes;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    // Addresses (loaded on show)
    private List<AddressDTO> addresses;

    // Current salary record (loaded on show)
    private SalaryDTO currentSalary;

    // ── Nested: address ────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AddressDTO {
        private Long   id;
        /** PRESENT | PERMANENT | OFFICE */
        private String addressType;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String district;
        @Builder.Default private String country = "Bangladesh";
        private String postalCode;
        @Builder.Default private Boolean isDefault = false;
    }

    // ── Nested: salary ─────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SalaryDTO {
        private Long       id;
        private LocalDate  effectiveDate;
        private LocalDate  endDate;
        private BigDecimal basicSalary;
        @Builder.Default private BigDecimal houseRent          = BigDecimal.ZERO;
        @Builder.Default private BigDecimal medicalAllowance   = BigDecimal.ZERO;
        @Builder.Default private BigDecimal transportAllowance = BigDecimal.ZERO;
        @Builder.Default private BigDecimal otherAllowances    = BigDecimal.ZERO;
        private BigDecimal grossSalary;
        @Builder.Default private BigDecimal incomeTax         = BigDecimal.ZERO;
        @Builder.Default private BigDecimal providentFund     = BigDecimal.ZERO;
        @Builder.Default private BigDecimal otherDeductions   = BigDecimal.ZERO;
        private BigDecimal netSalary;
        @Builder.Default private Boolean isCurrent = true;
        private String remarks;
        private String createdAt;
    }
}
