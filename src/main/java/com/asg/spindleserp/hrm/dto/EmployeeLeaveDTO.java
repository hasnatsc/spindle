package com.asg.spindleserp.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeLeaveDTO {
    private Long   id;

    @NotNull(message = "Employee is required")
    private Long   employeeId;
    private String employeeDisplay;

    /** ANNUAL | SICK | CASUAL | MATERNITY | PATERNITY | UNPAID | OTHER */
    @NotBlank(message = "Leave type is required")
    private String leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private BigDecimal totalDays;
    private String reason;

    /** PENDING | APPROVED | REJECTED | CANCELLED */
    @Builder.Default private String status = "PENDING";

    private String approvedBy;
    private String approvedAt;
    private Long   approvalRequestId;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
