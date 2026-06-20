package com.asg.spindleserp.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRunDTO {
    private Long   id;

    @NotBlank(message = "Payroll month is required (YYYY-MM)")
    private String payrollMonth;

    @NotNull(message = "Run date is required")
    private LocalDate runDate;

    /** DRAFT | PROCESSING | COMPLETED | APPROVED | PAID | CANCELLED */
    @Builder.Default private String status = "DRAFT";

    @Builder.Default private BigDecimal totalGross      = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalNet        = BigDecimal.ZERO;
    @Builder.Default private Integer    employeeCount   = 0;

    private String approvedBy;
    private String approvedAt;
    private String remarks;
    private Long   journalEntryId;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    private List<LineDTO> lines;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long   id;
        private Long   employeeId;
        private String employeeCode;
        private String employeeName;
        private String departmentName;
        private String designationName;
        private Long   costCenterId;
        private String costCenterDisplay;

        @Builder.Default private BigDecimal basicSalary        = BigDecimal.ZERO;
        @Builder.Default private BigDecimal houseRent          = BigDecimal.ZERO;
        @Builder.Default private BigDecimal medicalAllowance   = BigDecimal.ZERO;
        @Builder.Default private BigDecimal transportAllowance = BigDecimal.ZERO;
        @Builder.Default private BigDecimal overtime           = BigDecimal.ZERO;
        @Builder.Default private BigDecimal otherAllowances    = BigDecimal.ZERO;
        @Builder.Default private BigDecimal grossSalary        = BigDecimal.ZERO;
        @Builder.Default private BigDecimal incomeTax          = BigDecimal.ZERO;
        @Builder.Default private BigDecimal providentFund      = BigDecimal.ZERO;
        @Builder.Default private BigDecimal loanDeduction      = BigDecimal.ZERO;
        @Builder.Default private BigDecimal otherDeductions    = BigDecimal.ZERO;
        @Builder.Default private BigDecimal netSalary          = BigDecimal.ZERO;

        private Integer workingDays;
        private Integer leaveDays;
        private Integer absentDays;

        /** PENDING | PAID | CANCELLED */
        @Builder.Default private String paymentStatus = "PENDING";
    }
}
