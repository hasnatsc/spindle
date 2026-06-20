package com.asg.spindleserp.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {

    /* ============================================================
       PURCHASE
       ============================================================ */

    PURCHASE_REQUISITION("SPR", "Purchase Requisition"),
    REQUEST_FOR_QUOTATION("RFQ", "Request For Quotation"),
    COMPARATIVE_STATEMENT("CS", "Comparative Statement"),
    PURCHASE_ORDER("PO", "Purchase Order"),
    GOODS_RECEIPT_NOTE("GRN", "Goods Receipt Note"),
    PURCHASE_INVOICE("PI", "Purchase Invoice"),

    /* ============================================================
       SALES
       ============================================================ */

    SALES_QUOTATION("SQ", "Sales Quotation"),
    SALES_ORDER("SO", "Sales Order"),
    DELIVERY_ORDER("DO", "Delivery Order"),
    DELIVERY_CHALLAN("DC", "Delivery Challan"),
    SALES_INVOICE("SI", "Sales Invoice"),

    /* ============================================================
       STORE / INVENTORY
       ============================================================ */

    STORE_REQUISITION("SR", "Store Requisition"),
    MATERIAL_ISSUE("MI", "Material Issue"),
    MATERIAL_RECEIVE("MR", "Material Receive"),
    STOCK_TRANSFER("ST", "Stock Transfer"),
    STOCK_ADJUSTMENT("SA", "Stock Adjustment"),

    /* ============================================================
       PRODUCTION
       ============================================================ */

    PRODUCTION_ORDER("PROD", "Production Order"),
    PRODUCTION_REQUISITION("PRQ", "Production Requisition"),
    PRODUCTION_MATERIAL_ISSUE("PMI", "Production Material Issue"),
    FINISHED_GOODS_RECEIVE("FGR", "Finished Goods Receive"),

    /* ============================================================
       FINANCIAL
       ============================================================ */

    DEBIT_NOTE("DN", "Debit Note"),
    CREDIT_NOTE("CN", "Credit Note"),

    /* ============================================================
       COMMERCIAL
       ============================================================ */

    EXPORT_PROFORMA_INVOICE("EPI", "Export Proforma Invoice"),
    IMPORT_PROFORMA_INVOICE("IPI", "Import Proforma Invoice"),
    LETTER_OF_CREDIT("LC", "Letter Of Credit");

    private final String code;
    private final String displayName;

    public String nextDocumentNo(long sequence, String orgCode) {
        return code + "-"+ orgCode + "-" + java.time.Year.now().getValue() + "-" + String.format("%06d", sequence);
    }
}