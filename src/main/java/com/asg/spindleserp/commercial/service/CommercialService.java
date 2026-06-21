package com.asg.spindleserp.commercial.service;

import com.asg.spindleserp.commercial.dto.*;
import com.asg.spindleserp.commercial.entity.*;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.Map;

/**
 * CommercialService — unified service for the Commercial module.
 *
 * Covers:
 *   Export Commercial Invoices  (InvoiceType = EXPORT) → /commercial/exports
 *   Import Commercial Invoices  (InvoiceType = IMPORT) → /commercial/imports
 *   LC Settlements              (tied to an LC sub-account)
 *
 * Both export and import share the same entity (CommercialInvoice) and service
 * methods; the invoiceType discriminates between them.
 *
 * Commercial Invoice lifecycle:
 *   DRAFT → FINALIZED → POSTED → CANCELLED
 */
public interface CommercialService {

    // ── Commercial Invoice (shared for export + import) ───────────────────────

    CommercialInvoiceDTO createInvoice(CommercialInvoiceDTO dto);
    CommercialInvoiceDTO updateInvoice(Long id, CommercialInvoiceDTO dto);
    CommercialInvoiceDTO findInvoiceById(Long id);
    void                 deleteInvoice(Long id);

    CommercialInvoiceDTO finalizeInvoice(Long id);
    CommercialInvoiceDTO postInvoice(Long id);
    CommercialInvoiceDTO cancelInvoice(Long id, String remarks);

    /**
     * @param invoiceType EXPORT | IMPORT — filters the DataTable
     */
    DataTableResponse invoiceDatatable(int draw, int start, int length, String search, String invoiceType);
    Map<String, Object> searchInvoices(String q, String invoiceType, int page);

    CommercialInvoiceDTO toDTO(CommercialInvoice entity);

    // ── LC Settlement ─────────────────────────────────────────────────────────

    LcSettlementDTO createSettlement(LcSettlementDTO dto);
    LcSettlementDTO updateSettlement(Long id, LcSettlementDTO dto);
    LcSettlementDTO findSettlementById(Long id);
    void            deleteSettlement(Long id);
    LcSettlementDTO settleSettlement(Long id);
    LcSettlementDTO reverseSettlement(Long id);
    DataTableResponse settlementDatatable(int draw, int start, int length, String search, Long lcId);
    LcSettlementDTO toDTO(LcSettlement entity);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    Map<String, Object> dashboardSummary();
}
