package com.asg.spindleserp.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** ApprovalDelegationDTO — delegate approval authority to another user */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalDelegationDTO {
    private Long   id;
    private String delegationCode;       // auto-generated

    // Delegator (current user, set server-side)
    private Long   delegatorId;
    private String delegatorDisplay;

    // Delegate — AJAX Select2 → /users/search
    @NotNull(message = "Delegate user is required")
    private Long   delegateId;
    private String delegateDisplay;

    private String module;
    private String documentType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private BigDecimal maxAmount;
    private String reason;

    /** SCHEDULED | ACTIVE | EXPIRED | REVOKED */
    @Builder.Default private String status = "SCHEDULED";
    @Builder.Default private Boolean active             = true;
    @Builder.Default private Boolean notifyDelegator    = false;

    private String revocationReason;
    private String revokedAt;
    private String revokedByDisplay;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
