package com.asg.spindleserp.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PurchaseDocumentDTO — unified DTO for Purchase Cycle.
 *
 * documentType discriminates the form:
 *   PURCHASE_ORDER        → PO (no stock movement)
 *   GOODS_RECEIPT_NOTE    → GRN (stock IN — created from PO or fresh)
 *   PURCHASE_INVOICE      → Invoice (created from GRN or fresh)
 *   DEBIT_NOTE            → Return to supplier (stock OUT)
 *
 * Cascade chain (can populate next doc from DataTable row):
 *   PO ──► GRN ──► PURCHASE_INVOICE ──► DEBIT_NOTE (optional)
 *                                      └─► PAYMENT (via JournalEntryMaster / VoucherService)
 *
 * Status lifecycle:  DRAFT → CONFIRMED → CANCELLED | CLOSED
 * Stock is posted only on GRN confirm and Debit Note confirm.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseDocumentDTO {

    private Long   id;
    private String documentNo;
    private String documentNoManual;

    @NotNull(message = "Document type is required")
    private String documentType;

    @NotNull(message = "Document date is required")
    private LocalDate documentDate;

    /** DRAFT | CONFIRMED | CANCELLED | CLOSED */
    @Builder.Default private String status = "DRAFT";

    // ── Parent document (for cascade populate) ────────────────────────────────
    private Long   parentDocumentId;
    private String parentDocumentNo;
    private String parentDocumentType;

    // ── Supplier — AJAX Select2 ───────────────────────────────────────────────
    @NotNull(message = "Supplier is required")
    private Long   partyId;
    private String partyDisplay;       // "{code} — {name}"

    // ── Warehouse — AJAX Select2 ──────────────────────────────────────────────
    private Long   warehouseId;
    private String warehouseDisplay;

    // ── Reference / shipping ─────────────────────────────────────────────────
    @Size(max = 100) private String referenceNo;
    @Size(max = 100) private String supplierInvoiceNo;  // for Purchase Invoice
    private LocalDate supplierInvoiceDate;

    @Size(max = 50)  private String currency;
    private BigDecimal exchangeRate;

    @Size(max = 50)  private String incoterms;
    @Size(max = 50)  private String portOfLoading;
    @Size(max = 50)  private String portOfDischarge;
    @Size(max = 100) private String vesselName;
    @Size(max = 100) private String blNumber;
    @Size(max = 100) private String containerNumber;

    // ── Delivery / dates ──────────────────────────────────────────────────────
    private LocalDate requiredDate;
    private LocalDate deliveryDate;
    private LocalDate validityDate;

    // ── Amounts ───────────────────────────────────────────────────────────────
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal otherCharges;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;

    // ── Flags ─────────────────────────────────────────────────────────────────
    @Builder.Default private Boolean stockPosted      = false;
    @Builder.Default private Boolean accountingPosted = false;

    // ── Notes ─────────────────────────────────────────────────────────────────
    @Size(max = 65535) private String termsAndConditions;
    @Size(max = 65535) private String remarks;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;

    // ── Lines ─────────────────────────────────────────────────────────────────
    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<LineDTO> lines;

    // ─────────────────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long    id;
        private Long    sourceLineId;    // FK to parent document line (for cascade)
        private Integer lineNumber;

        // Item — AJAX Select2
        @NotNull(message = "Item is required on each line")
        private Long   itemId;
        private String itemCode;
        private String itemName;
        private String unitCode;

        // Lot — optional; set on GRN or Debit Note
        private Long   lotId;
        private String lotNumber;

        // Quantities
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        private BigDecimal deliveredQty;
        private BigDecimal receivedQty;
        private BigDecimal acceptedQty;
        private BigDecimal rejectedQty;

        // Pricing
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal lineAmount;

        @Size(max = 100) private String batchNumber;
        private LocalDate expectedDate;

        @Size(max = 30)    private String qualityStatus;
        @Size(max = 65535) private String qualityRemarks;
        @Size(max = 65535) private String remarks;

        // Lot creation fields (GRN creates lots on confirm)
        private Boolean createLot;        // true = auto-create InventoryLot on GRN confirm
        private LocalDate lotExpiryDate;
        private LocalDate lotManufacturingDate;
    }
}
