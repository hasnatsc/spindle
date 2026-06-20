package com.asg.spindleserp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SalesDocumentDTO — unified DTO for the Sales Cycle.
 *
 * documentType discriminates the form:
 *   SALES_ORDER        → SO (no stock movement)
 *   DELIVERY_ORDER     → Delivery Note (stock OUT — SALES_ISSUE)
 *   SALES_INVOICE      → Invoice (creates AR receivable)
 *   CREDIT_NOTE        → Customer return (stock IN — CUSTOMER_RETURN)
 *
 * Cascade chain (populate next doc from DataTable row):
 *   SO ──► Delivery Note ──► SALES_INVOICE ──► CREDIT_NOTE (optional)
 *                                             └─► RECEIPT (via VoucherService)
 *
 * Party = CUSTOMER (acc_chart_of_accounts_sub, subAccountType = CUSTOMER)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesDocumentDTO {

    private Long   id;
    private String documentNo;
    private String documentNoManual;

    @NotNull(message = "Document type is required")
    private String documentType;

    @NotNull(message = "Document date is required")
    private LocalDate documentDate;

    @Builder.Default private String status = "DRAFT";

    // ── Parent document (cascade) ──────────────────────────────────────────────
    private Long   parentDocumentId;
    private String parentDocumentNo;
    private String parentDocumentType;

    // ── Customer — AJAX Select2 ───────────────────────────────────────────────
    @NotNull(message = "Customer is required")
    private Long   partyId;
    private String partyDisplay;        // "{code} — {name}"
    private BigDecimal partyBalance;    // current AR balance

    // ── Warehouse (source of stock for Delivery) ─────────────────────────────
    private Long   warehouseId;
    private String warehouseDisplay;

    // ── Reference / shipping ─────────────────────────────────────────────────
    @Size(max = 100) private String referenceNo;
    @Size(max = 100) private String customerPoNo;    // customer's own PO reference
    @Size(max = 50)  private String currency;
    private BigDecimal exchangeRate;

    @Size(max = 50)  private String incoterms;
    @Size(max = 50)  private String portOfLoading;
    @Size(max = 50)  private String portOfDischarge;
    @Size(max = 100) private String vesselName;
    @Size(max = 100) private String blNumber;
    @Size(max = 100) private String containerNumber;
    @Size(max = 100) private String challanNo;
    @Size(max = 100) private String vehicleNumber;
    @Size(max = 100) private String driverName;
    @Size(max = 500) private String deliveryAddress;

    // ── Dates ─────────────────────────────────────────────────────────────────
    private LocalDate requiredDate;
    private LocalDate deliveryDate;
    private LocalDate validityDate;

    // ── Amounts ───────────────────────────────────────────────────────────────
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
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
        private Long    sourceLineId;
        private Integer lineNumber;

        // Item — AJAX Select2
        @NotNull(message = "Item is required on each line")
        private Long   itemId;
        private String itemCode;
        private String itemName;
        private String unitCode;

        // Lot — available stock lot (for Delivery)
        private Long   lotId;
        private String lotNumber;
        private BigDecimal availableQty;   // for display only

        // Quantities
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        private BigDecimal deliveredQty;
        private BigDecimal returnedQty;

        // Pricing
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal lineAmount;

        // Delivery extras
        private BigDecimal grossWeight;
        private BigDecimal netWeight;

        @Size(max = 30)    private String qualityStatus;
        @Size(max = 65535) private String remarks;
    }
}
