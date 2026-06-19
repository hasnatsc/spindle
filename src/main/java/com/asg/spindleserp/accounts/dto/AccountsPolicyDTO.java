package com.asg.spindleserp.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * AccountsPolicyDTO — canonical pattern.
 * Mapping FK and two default account FKs use AJAX Select2.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsPolicyDTO {

    private Long id;

    // ── AJAX Select2 FKs ──────────────────────────────────────────────────────
    private Long   accountsMappingId;          private String accountsMappingDisplay;
    private Long   defaultDebitAccountId;      private String defaultDebitAccountDisplay;
    private Long   defaultCreditAccountId;     private String defaultCreditAccountDisplay;

    @NotBlank(message = "Policy code is required")
    @Size(max = 30)
    private String policyCode;

    @NotBlank(message = "Policy name is required")
    @Size(max = 200)
    private String policyName;

    @NotBlank(message = "Policy type is required")
    @Size(max = 30)
    private String policyType;   // JOURNAL_VOUCHER | PAYMENT_VOUCHER | ...

    @Size(max = 30)  private String moduleType;
    @Size(max = 500) private String description;
    @Size(max = 20)  private String voucherPrefix;

    private Integer nextVoucherNumber;
    @Builder.Default private Integer numberPadding = 6;

    private BigDecimal approvalThreshold;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;

    @Size(max = 500) private String defaultNarrationTemplate;

    private Integer backdatingDays;

    // ── Flags ─────────────────────────────────────────────────────────────────
    @Builder.Default private Boolean autoNumbering       = true;
    @Builder.Default private Boolean autoPost            = false;
    @Builder.Default private Boolean requireApproval     = false;
    @Builder.Default private Boolean allowBackdating     = false;
    @Builder.Default private Boolean allowFutureDating   = false;
    @Builder.Default private Boolean allowEditAfterPost  = false;
    @Builder.Default private Boolean allowReversal       = true;
    @Builder.Default private Boolean allowNegativeAmount = false;
    @Builder.Default private Boolean allowZeroAmount     = false;
    @Builder.Default private Boolean active              = true;
    @Builder.Default private Boolean isDefault           = false;
    @Builder.Default private Boolean isSystem            = false;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
