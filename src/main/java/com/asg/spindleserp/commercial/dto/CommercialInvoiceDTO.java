package com.asg.spindleserp.commercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CommercialInvoiceDTO — covers both EXPORT and IMPORT commercial invoices.
 *
 * Linked to:
 *   lc       → ChartOfAccountSub (LC account)   — AJAX Select2
 *   party    → ChartOfAccountSub (buyer/seller)  — AJAX Select2
 *
 * InvoiceType  = EXPORT | IMPORT
 * InvoiceStatus = DRAFT | FINALIZED | POSTED | CANCELLED
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommercialInvoiceDTO {
    private Long   id;
    private String invoiceNo;           // auto-generated

    private Long   lcId;
    private String lcDisplay;           // "{code} — {name}"

    private Long   partyId;
    private String partyDisplay;

    @NotBlank(message = "Invoice type is required")
    private String invoiceType;         // EXPORT | IMPORT

    @Builder.Default private String status = "DRAFT";

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotBlank(message = "Currency is required")
    private String currency;

    @Builder.Default private BigDecimal exchangeRate   = BigDecimal.ONE;
    private BigDecimal totalAmount;
    private BigDecimal totalAmountBdt;

    // Shipping / logistic fields
    private String incoterms;
    private String portOfLoading;
    private String portOfDischarge;
    private String vesselName;
    private String blNumber;
    private String containerNo;

    // Stub FKs — delivery or GRN reference
    private Long   deliveryId;
    private Long   grnId;

    private String remarks;
    private String createdAt; private String updatedAt;

    @Valid
    private List<ItemLineDTO> items;

    /** Document terms attached to this invoice */
    private List<TermLineDTO> terms;

    // ── Item line ─────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemLineDTO {
        private Long   id;
        // Item — AJAX Select2 → /inventory/items/search
        @NotNull(message = "Item is required")
        private Long   itemId;
        private String itemDisplay;
        private String unit;
        private String description;
        @NotNull private BigDecimal quantity;
        @NotNull private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private Long   deliveryDetailId;
    }

    // ── Terms line ────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TermLineDTO {
        private Long    id;
        private Long    globalTermsId;
        @NotBlank private String title;
        private String  description;
        private Integer sortOrder;
    }
}
