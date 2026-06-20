package com.asg.spindleserp.approval.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

// ── ApprovalConfigDTO ─────────────────────────────────────────────────────────
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalConfigDTO {
    private Long   id;

    @NotBlank(message = "Code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "Document type is required")
    private String documentType;

    @NotBlank(message = "Module is required")
    private String module;

    /** SEQUENTIAL | PARALLEL */
    @Builder.Default private String flowType = "SEQUENTIAL";

    @Builder.Default private Boolean active                  = true;
    @Builder.Default private Boolean enableReminders         = false;
    @Builder.Default private Boolean useReportingHierarchy   = false;

    private Integer    priority;
    private Integer    autoEscalationHours;
    private Integer    reminderIntervalHours;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    @Valid
    private List<LevelDTO> levels;

    // ── Nested level DTO ──────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LevelDTO {
        private Long id;

        @NotNull(message = "Level number is required")
        private Integer levelNumber;

        @NotBlank(message = "Level name is required")
        @Size(max = 100)
        private String levelName;

        @Size(max = 500) private String description;
        @Size(max = 200) private String approverDescription;

        // Approver user — AJAX Select2 → /users/search
        private Long   approverUserId;
        private String approverUserDisplay;

        @Builder.Default private Boolean active                = true;
        @Builder.Default private Boolean canApproveWithChanges = false;
        @Builder.Default private Boolean canDelegate           = false;
        @Builder.Default private Boolean canForward            = false;
        @Builder.Default private Boolean canHold               = false;
    }
}
