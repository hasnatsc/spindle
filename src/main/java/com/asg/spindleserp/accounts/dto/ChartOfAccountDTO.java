package com.asg.spindleserp.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * ChartOfAccountDTO
 *
 * Transfer object for ChartOfAccount CRUD.
 * Follows the BusinessUnit canonical pattern:
 *  - @Builder.Default active = true
 *  - Boolean wrapper (nullable)
 *  - Parent account resolved by id for tree display
 *  - AJAX Select2 fields: parentAccountId + parentAccountDisplay
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccountDTO {

    private Long id;

    // ── Parent (AJAX Select2) ─────────────────────────────────────────────────
    private Long   parentAccountId;
    private String parentAccountDisplay;   // "{code} — {name}"

    // ── Identity ──────────────────────────────────────────────────────────────
    @NotBlank(message = "Account code is required")
    @Size(max = 50)
    private String accountCode;

    @NotBlank(message = "Account name is required")
    @Size(max = 200)
    private String accountName;

    /** ASSET | LIABILITY | EQUITY | REVENUE | EXPENSE */
    @NotBlank(message = "Account type is required")
    private String accountType;

    /** DEBIT | CREDIT */
    @NotBlank(message = "Account nature is required")
    private String accountNature;

    private Integer level;

    // ── Balances ──────────────────────────────────────────────────────────────
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;

    @Size(max = 10)
    private String currency;

    @Size(max = 1000)
    private String description;

    @Size(max = 50)
    private String taxId;

    // ── Flags ─────────────────────────────────────────────────────────────────
    @Builder.Default private Boolean active           = true;
    @Builder.Default private Boolean isSystem         = false;
    @Builder.Default private Boolean isControlAccount = false;
    @Builder.Default private Boolean allowManualEntry = true;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
