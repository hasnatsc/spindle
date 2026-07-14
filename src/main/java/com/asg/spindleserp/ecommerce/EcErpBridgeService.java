// Path: com/asg/spindleserp/ecommerce/service/EcErpBridgeService.java
package com.asg.spindleserp.ecommerce;

/**
 * EcErpBridgeService — ecommerce-to-ERP integration bridge.
 *
 * Orchestrates the creation of ERP documents and GL journal entries
 * from ecommerce lifecycle events.
 *
 * Event-to-document mapping is configured in ec_document_mapping:
 *   ORDER_CONFIRMED → SALES_INVOICE
 *   PAYMENT_SUCCESS → RECEIPT_VOUCHER
 *   ORDER_SHIPPED   → DELIVERY_CHALLAN
 *   RETURN_APPROVED → CREDIT_NOTE
 *   REFUND_ISSUED   → PAYMENT_VOUCHER
 *
 * GL account defaults are configured in ec_gl_account_defaults
 * (per-organization) via EcGlAccountDefaultsService.
 *
 * Lookup priority (from EcGlAccountDefaults):
 *   1. ec_gl_account_defaults for org
 *   2. acc_mapping if ECOMMERCE module_type exists
 *   3. First matching account_type
 */
public interface EcErpBridgeService {

    /**
     * Process an ecommerce event and create the corresponding ERP document(s).
     *
     * @param orgId       the organization ID
     * @param eventType   the ecommerce event type (e.g. 'ORDER_CONFIRMED')
     * @param sourceRef   the reference identifier of the source entity
     *                    (e.g. EcOrder.orderNo, EcPayment.transactionId)
     * @param sourceId    the primary key of the source entity
     * @return the created/updated ERP document ID, or null if no mapping applies
     */
    Long processEvent(Long orgId, String eventType, String sourceRef, Long sourceId);

    /**
     * Synchronously create a Sales Invoice (global_business_documents) from an ecommerce order.
     * Called when ORDER_CONFIRMED event fires.
     *
     * @param orgId   organization ID
     * @param orderNo ecommerce order number
     * @param orderId EcOrder PK
     * @return created ERP BusinessDocument ID
     */
    Long createSalesInvoiceFromOrder(Long orgId, String orderNo, Long orderId);

    /**
     * Create a Receipt Voucher (acc_journal_entry_master) from a confirmed payment.
     * Called when PAYMENT_SUCCESS event fires.
     *
     * @param orgId         organization ID
     * @param transactionId payment transaction reference
     * @param paymentId     EcPayment PK
     * @return created Voucher ID
     */
    Long createReceiptFromPayment(Long orgId, String transactionId, Long paymentId);

    /**
     * Create ERP documents mapped from an ecommerce event type.
     * Uses ec_document_mapping to determine the target document type.
     *
     * @param orgId     organization ID
     * @param eventType ecommerce event type
     * @param sourceRef reference identifier
     * @param sourceId  source entity PK
     * @return created ERP document ID, or null if event type is not mapped
     */
    Long createDocumentFromMapping(Long orgId, String eventType, String sourceRef, Long sourceId);
}
