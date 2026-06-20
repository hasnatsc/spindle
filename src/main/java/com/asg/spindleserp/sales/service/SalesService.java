package com.asg.spindleserp.sales.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.sales.dto.SalesDocumentDTO;

import java.util.List;
import java.util.Map;

/**
 * SalesService
 *
 * Manages the full sales cycle:
 *   SALES_ORDER → DELIVERY_ORDER → SALES_INVOICE → CREDIT_NOTE
 *
 * Mirror of PurchaseService pattern.
 *
 * Stock movements:
 *   Delivery confirm → SALES_ISSUE   (outbound — decreases stock)
 *   Credit Note confirm → CUSTOMER_RETURN  (inbound — increases stock)
 *
 * AR:
 *   Sales Invoice confirm → increases customer's currentBalance (receivable)
 *   Credit Note confirm   → decreases customer's currentBalance
 */
public interface SalesService {

    SalesDocumentDTO save(SalesDocumentDTO dto);

    SalesDocumentDTO findById(Long id);

    void delete(Long id);

    /**
     * Confirms the document:
     *  - SO       → status = CONFIRMED (no stock movement, optional reservation)
     *  - Delivery → posts SALES_ISSUE inventory transactions (stock OUT)
     *  - Invoice  → creates AR receivable on customer's sub-account
     *  - CN       → posts CUSTOMER_RETURN (stock IN), reduces AR
     */
    SalesDocumentDTO confirm(Long id);

    SalesDocumentDTO cancel(Long id);

    /**
     * Populates a child document from a confirmed parent.
     * SO → Delivery:  copies quantity → deliveredQty default
     * Delivery → Invoice: copies deliveredQty → quantity
     * Invoice → Credit Note: copies quantity for return
     */
    SalesDocumentDTO populateFromSource(Long parentId, String childType);

    DataTableResponse datatableList(String documentType, int draw, int start, int length, String search);

    /** Confirmed SOs for a customer — for Delivery "select SO" picker */
    List<Map<String, Object>> openSOsForCustomer(Long customerId);

    /** Confirmed Deliveries for a customer — for Invoice picker */
    List<Map<String, Object>> confirmedDeliveriesForCustomer(Long customerId);

    SalesDocumentDTO toDTO(BusinessDocument entity);
}
