package com.asg.spindleserp.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** ApprovalRequestDTO — tracks one approval workflow instance */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequestDTO {
    private Long   id;

    // Config
    private Long   approvalConfigId;
    private String approvalConfigDisplay;

    // Linked document
    @NotBlank(message = "Document type is required")
    private String documentType;
    @NotNull(message = "Reference ID is required")
    private Long   referenceId;
    @NotBlank(message = "Reference number is required")
    private String referenceNumber;
    private LocalDate documentDate;
    private BigDecimal documentAmount;
    private String documentSummary;

    // Requester (set server-side)
    private Long   requesterId;
    private String requesterName;

    // Current approval position
    private Long   currentApprovalLevelId;
    private Long   currentApproverUserId;
    private String currentApproverDisplay;
    private String currentApproverRole;
    private Integer currentLevelNumber;
    private Integer totalLevels;

    /** DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | REJECTED | RETURNED | CANCELLED | HOLD | COMPLETED */
    @Builder.Default private String status = "DRAFT";

    @Builder.Default private Boolean isUrgent = false;
    private LocalDate dueDate;

    private String finalRemarks;
    private String finalActionBy;
    private LocalDateTime completedAt;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    // History (loaded on show)
    private List<HistoryDTO> history;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HistoryDTO {
        private Long   id;
        private Integer levelNumber;
        private String levelName;
        private String actorName;
        private String actorDesignation;
        private String action;
        private String status;
        private String comments;
        private String rejectionReason;
        private String returnReason;
        private Boolean isAutoAction;
        private LocalDateTime actionAt;
    }
}
