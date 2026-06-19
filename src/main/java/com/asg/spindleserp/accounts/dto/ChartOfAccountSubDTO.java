package com.asg.spindleserp.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ChartOfAccountSubDTO
 *
 * Unified DTO for all ChartOfAccountSub sub-types:
 *   BANK | CASH | CUSTOMER | SUPPLIER | EMPLOYEE | LC | GENERAL | INTER_COMPANY
 *
 * Fields are conditionally required based on subAccountType.
 * AJAX Select2 fields:
 *   - mainAccountId / mainAccountDisplay  → /accounts/chart/search
 *   - bankId / bankName                   → /banks/search
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccountSubDTO {

    private Long id;

    // ── Parent account (AJAX Select2) ─────────────────────────────────────────
    @NotNull(message = "Main account is required")
    private Long   mainAccountId;
    private String mainAccountDisplay;   // "{code} — {name}"

    // ── Core identity ─────────────────────────────────────────────────────────
    @NotBlank(message = "Sub-account type is required")
    private String subAccountType;   // BANK | CASH | CUSTOMER | SUPPLIER | EMPLOYEE | LC | GENERAL

    @NotBlank(message = "Sub-account code is required")
    @Size(max = 50)
    private String subAccountCode;

    @NotBlank(message = "Sub-account name is required")
    @Size(max = 200)
    private String subAccountName;

    @Builder.Default private Boolean active = true;

    // ── Common contact / address ───────────────────────────────────────────────
    @Size(max = 200) private String contactPerson;
    @Size(max = 20)  private String contactPhone;
    @Size(max = 100) private String contactEmail;
    @Size(max = 500) private String address;
    @Size(max = 50)  private String city;
    @Size(max = 50)  private String state;
    @Size(max = 50)  private String country;
    @Size(max = 20)  private String postalCode;
    @Size(max = 50)  private String taxId;
    @Size(max = 50)  private String vatRegistrationNo;
    @Size(max = 20)  private String currency;
    @Size(max = 1000) private String description;
    @Size(max = 1000) private String remarks;

    private BigDecimal openingBalance;
    private BigDecimal currentBalance;

    // ── BANK-specific ─────────────────────────────────────────────────────────
    private Long   bankId;
    private String bankName;              // resolved display

    @Size(max = 50)  private String bankAccountCode;
    @Size(max = 50)  private String accountNumber;
    @Size(max = 200) private String accountTitle;
    @Size(max = 200) private String bankNameManual;
    @Size(max = 20)  private String bankAccountType;    // SAVINGS | CURRENT | STD
    @Size(max = 100) private String branchName;
    @Size(max = 10)  private String branchCode;
    @Size(max = 200) private String branchAddress;
    @Size(max = 20)  private String branchPhone;
    @Size(max = 9)   private String routingNumber;
    @Size(max = 11)  private String swiftCode;
    @Size(max = 34)  private String ibanNumber;
    private BigDecimal interestRate;
    private BigDecimal overdraftLimit;
    private BigDecimal overdraftInterestRate;

    // ── CASH-specific ─────────────────────────────────────────────────────────
    @Size(max = 50)  private String cashAccountCode;
    @Size(max = 20)  private String cashAccountType;
    @Size(max = 100) private String location;
    @Size(max = 100) private String custodian;
    @Size(max = 100) private String custodianEmail;
    @Size(max = 20)  private String custodianPhone;
    private BigDecimal maximumLimit;
    private BigDecimal minimumLimit;
    private BigDecimal approvalLimit;
    @Builder.Default private Boolean requiresApproval = false;

    // ── CUSTOMER-specific ─────────────────────────────────────────────────────
    @Size(max = 50)  private String customerCode;
    private BigDecimal creditLimit;
    @Size(max = 100) private String paymentTerms;
    private Integer creditDays;
    @Size(max = 100) private String salesRepresentative;
    @Size(max = 50)  private String customerGroup;
    private Integer loyaltyPoints;
    @Builder.Default private Boolean isExportCustomer = false;

    // ── SUPPLIER-specific ─────────────────────────────────────────────────────
    @Size(max = 50)  private String supplierCode;
    private Integer leadTimeDays;
    @Size(max = 3)   private String preferredCurrency;
    @Builder.Default private Boolean isImportSupplier = false;

    // ── LC-specific ───────────────────────────────────────────────────────────
    @Size(max = 100) private String lcNumber;
    @Size(max = 100) private String manualLcNumber;
    @Size(max = 30)  private String lcType;      // BTB | MASTER | SIGHT | USANCE
    @Size(max = 30)  private String lcStatus;    // OPEN | SHIPPED | SETTLED | EXPIRED | CANCELLED
    @Size(max = 20)  private String transactionCurrency;
    private BigDecimal lcAmount;
    private BigDecimal exchangeRate;
    private LocalDate  issueDate;
    private LocalDate  expiryDate;
    private LocalDate  shipmentDate;
    private LocalDate  receivingDate;
    private Integer    tenureDays;
    @Size(max = 100) private String masterLcNo;
    @Size(max = 100) private String btbLcNo;
    private LocalDate  masterLcDate;
    @Size(max = 30)  private String paymentTerm;
    @Size(max = 20)  private String shipmentMode;
    private Boolean partialShipmentAllowed;
    private Boolean btmaCertificateRequired;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
