package com.asg.spindleserp.hrm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

// ── AttendanceDTO ──────────────────────────────────────────────────────────────
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceDTO {
    private Long   id;

    @NotNull(message = "Employee is required")
    private Long   employeeId;
    private String employeeDisplay;

    @NotNull(message = "Attendance date is required")
    private LocalDate attDate;

    private LocalTime checkIn;
    private LocalTime checkOut;
    private BigDecimal workingHours;

    /** PRESENT | ABSENT | LATE | HALF_DAY | HOLIDAY | LEAVE | WEEKEND */
    @Builder.Default private String status = "ABSENT";
    /** MANUAL | DEVICE | IMPORT */
    @Builder.Default private String source = "MANUAL";

    private String remarks;
    private String createdAt; private String updatedAt;
    private String createdBy;
}
