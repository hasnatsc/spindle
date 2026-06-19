package com.asg.spindleserp.accounts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * AccountsMappingDTO — header + detail lines (CascadeAll).
 * All ChartOfAccount FK fields use AJAX Select2 pattern: id + display text.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsMappingDTO {

    private Long id;

    @NotBlank(message = "Mapping code is required")
    @Size(max = 30)
    private String mappingCode;

    @NotBlank(message = "Mapping name is required")
    @Size(max = 200)
    private String mappingName;

    @NotBlank(message = "Module type is required")
    @Size(max = 50)
    private String moduleType;         // GENERAL_LEDGER | ACCOUNTS_PAYABLE | ...

    @NotBlank(message = "Transaction type is required")
    @Size(max = 50)
    private String transactionType;    // PURCHASE_INVOICE | SALES_INVOICE | ...

    @Size(max = 500)
    private String description;

    @Size(max = 30)  private String defaultVoucherType;
    @Size(max = 30)  private String voucherType;
    @Size(max = 20)  private String voucherPrefix;

    @Builder.Default private String debitControlType  = "NONE";
    @Builder.Default private String creditControlType = "NONE";

    @Size(max = 500) private String defaultNarrationTemplate;

    // ── AJAX Select2 accounts ─────────────────────────────────────────────────
    private Long   defaultDebitAccountId;    private String defaultDebitAccountDisplay;
    private Long   defaultCreditAccountId;   private String defaultCreditAccountDisplay;
    private Long   discountAccountId;        private String discountAccountDisplay;
    private Long   freightAccountId;         private String freightAccountDisplay;
    private Long   inputVatAccountId;        private String inputVatAccountDisplay;
    private Long   outputVatAccountId;       private String outputVatAccountDisplay;
    private Long   forexGainAccountId;       private String forexGainAccountDisplay;
    private Long   forexLossAccountId;       private String forexLossAccountDisplay;
    private Long   tdsAccountId;             private String tdsAccountDisplay;
    private Long   aitAccountId;             private String aitAccountDisplay;
    private Long   roundingAccountId;        private String roundingAccountDisplay;

    // ── Flags ─────────────────────────────────────────────────────────────────
    @Builder.Default private Boolean useSubLedger            = false;
    @Builder.Default private Boolean updateSubAccountBalance = false;
    @Builder.Default private Boolean allowPartialPosting     = false;
    @Builder.Default private Boolean autoPost                = false;
    @Builder.Default private Boolean consolidateEntries      = false;
    @Builder.Default private Boolean createReversingEntry    = false;
    @Builder.Default private Boolean requireApproval         = false;
    @Builder.Default private Boolean active                  = true;
    @Builder.Default private Boolean isDefault               = false;
    @Builder.Default private Boolean isSystem                = false;

    @Valid
    private List<DetailDTO> details;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;

    // ── Detail line ───────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DetailDTO {
        private Long    id;
        private Integer lineNumber;

        // AJAX Select2
        private Long   accountId;    private String accountDisplay;
        private Long   costCenterId; private String costCenterDisplay;

        @NotBlank(message = "Entry type is required")
        private String entryType;   // DEBIT | CREDIT

        @NotBlank(message = "Amount type is required")
        private String amountType;  // FULL_AMOUNT | FIXED_PERCENTAGE | FIXED_AMOUNT | ...

        private BigDecimal percentage;
        private BigDecimal fixedAmount;

        @Size(max = 500)  private String formula;
        @Size(max = 100)  private String fieldReference;
        @Size(max = 100)  private String entryName;
        @Size(max = 500)  private String entryDescription;
        @Size(max = 500)  private String condition;
        @Size(max = 20)   private String conditionOperator;
        @Size(max = 200)  private String conditionValue;
        @Size(max = 30)   private String controlAccountType;
        @Size(max = 500)  private String lineNarration;
        @Size(max = 20)   private String taxCode;
        private BigDecimal taxRate;
        private Integer    sortOrder;

        @Builder.Default private Boolean negateAmount    = false;
        @Builder.Default private Boolean roundAmount     = false;
        @Builder.Default private Boolean skipIfZero      = false;
        @Builder.Default private Boolean isTaxEntry      = false;
        @Builder.Default private Boolean isOptional      = false;
        @Builder.Default private Boolean active          = true;
        @Builder.Default private Boolean inheritCostCenter = false;
    }
}
