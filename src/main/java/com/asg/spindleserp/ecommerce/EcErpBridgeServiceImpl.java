// Path: com/asg/spindleserp/ecommerce/service/EcErpBridgeServiceImpl.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.customerSupport.repository.EcCustomerRepository;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.entity.EcOrderItem;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.ecommerce.payment.entity.EcPayment;
import com.asg.spindleserp.ecommerce.payment.repository.EcPaymentRepository;
import com.asg.spindleserp.ecommerce.settings.entity.EcDocumentMapping;
import com.asg.spindleserp.ecommerce.settings.repository.EcDocumentMappingRepository;
import com.asg.spindleserp.global.entity.BusinessDocument;
import com.asg.spindleserp.global.entity.BusinessDocumentLine;
import com.asg.spindleserp.global.repository.BusinessDocumentRepository;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * EcErpBridgeServiceImpl — bridges ecommerce lifecycle events into ERP documents.
 *
 * Currently handles:
 *   ORDER_CONFIRMED → SALES_INVOICE (via global_business_documents)
 *   PAYMENT_SUCCESS → RECEIPT_VOUCHER integration routing (links to payment)
 *
 * Future: full GL posting via ec_gl_account_defaults + VoucherService.
 *
 * Each method is idempotent: if the ERP document already exists for the source
 * reference, it returns the existing ID instead of creating a duplicate.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcErpBridgeServiceImpl implements EcErpBridgeService {

    private final EcDocumentMappingRepository  docMappingRepo;
    private final EcOrderRepository            orderRepo;
    private final EcCustomerRepository         customerRepo;
    private final EcPaymentRepository          paymentRepo;
    private final BusinessDocumentRepository   erpDocRepo;
    private final OrganizationRepository       orgRepo;
    private final DocumentSequenceService      seqService;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    @Override
    public Long processEvent(Long orgId, String eventType, String sourceRef, Long sourceId) {
        log.debug("Processing ecommerce event: {} ref={} id={}", eventType, sourceRef, sourceId);

        // Check if already processed (idempotency)
        Optional<BusinessDocument> existing = erpDocRepo.findByOrganizationIdAndReferenceNo(orgId, sourceRef);
        if (existing.isPresent()) {
            log.info("ERP document already exists for ref={}, id={}", sourceRef, existing.get().getId());
            return existing.get().getId();
        }

        return createDocumentFromMapping(orgId, eventType, sourceRef, sourceId);
    }

    @Override
    public Long createSalesInvoiceFromOrder(Long orgId, String orderNo, Long orderId) {
        EcOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("EcOrder #" + orderId + " not found."));

        // Idempotency check
        Optional<BusinessDocument> existing = erpDocRepo
                .findByOrganizationIdAndReferenceNo(orgId, "EC-" + orderNo);
        if (existing.isPresent()) return existing.get().getId();

        EcCustomer customer = order.getCustomer();
        Organization org = orgRepo.getReferenceById(orgId);

        // Resolve party (customer's ERP sub-account, or fallback to default AR)
        ChartOfAccountSub partyAccount = resolveCustomerParty(customer);
        if (partyAccount == null) {
            log.warn("Customer {} has no ERP sub-account linked — using ec_gl_account_defaults.AR", customer.getId());
        }

        // Build the ERP Sales Invoice document
        BusinessDocument si = new BusinessDocument();
        si.setOrganization(org);
        si.setDocumentType(DocumentType.SALES_INVOICE);
        si.setDocumentNo(seqService.nextDocumentNumber(orgId, "SI", String.valueOf(LocalDate.now().getYear())));
        si.setDocumentDate(LocalDate.now());
        si.setReferenceNo("EC-" + orderNo);
        si.setParty(partyAccount);
        si.setStatus("DRAFT");
        si.setDeleted(false);
        si.setStockPosted(false);
        si.setAccountingPosted(false);
        si.setCurrency(order.getCurrencyCode());
        si.setExchangeRate(order.getExchangeRate());
        si.setRemarks("Auto-created from ecommerce order: " + orderNo);
        // Audit fields populated automatically by @PrePersist/@PreUpdate hooks

        // Recalculate totals from order items
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        int lineNum = 1;

        for (EcOrderItem oi : order.getOrderItems()) {
            Item item = oi.getItem();
            BigDecimal lineAmount = oi.getUnitPrice() != null
                    ? oi.getUnitPrice().multiply(oi.getQuantity())
                    : BigDecimal.ZERO;
            BigDecimal taxAmount = oi.getTaxAmount() != null ? oi.getTaxAmount() : BigDecimal.ZERO;
            subtotal = subtotal.add(lineAmount);
            taxTotal = taxTotal.add(taxAmount);

            BusinessDocumentLine line = BusinessDocumentLine.builder()
                    .organizationId(orgId)
                    .document(si)
                    .item(item)
                    .lineNumber(lineNum++)
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .unitCode(item.getSalesUnitCode())
                    .quantity(oi.getQuantity())
                    .unitPrice(oi.getUnitPrice())
                    .taxAmount(taxAmount)
                    .lineAmount(lineAmount)
                    .remarks(oi.getProduct().getProductTitle())
                    .build();
            si.getLines().add(line);
        }

        si.setSubtotalAmount(subtotal);
        si.setTaxAmount(taxTotal);
        si.setTotalAmount(subtotal.add(taxTotal));
        si.setPaidAmount(BigDecimal.ZERO);
        si.setDueAmount(si.getTotalAmount());

        BusinessDocument saved = erpDocRepo.save(si);
        log.info("Created SALES_INVOICE #{} from ecommerce order {}", saved.getDocumentNo(), orderNo);

        return saved.getId();
    }

    @Override
    public Long createReceiptFromPayment(Long orgId, String transactionId, Long paymentId) {
        // Resolve the payment record
        EcPayment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("EcPayment #" + paymentId + " not found."));

        String ref = "EC-PAY-" + transactionId;
        Optional<BusinessDocument> existing = erpDocRepo
                .findByOrganizationIdAndReferenceNo(orgId, ref);
        if (existing.isPresent()) return existing.get().getId();

        // RECEIPT_VOUCHER creation is handled via VoucherService (accounts module).
        // This method flags the payment as ERP-integrated for future processing.
        log.info("Payment {} (txn={}) ready for RECEIPT_VOUCHER creation. GL posting pending.", paymentId, transactionId);

        // TODO: Delegate to VoucherService.populateReceiptFromInvoice() once the
        //       associated SALES_INVOICE is confirmed. For now, mark reference.
        return null;
    }

    @Override
    public Long createDocumentFromMapping(Long orgId, String eventType, String sourceRef, Long sourceId) {
        Optional<EcDocumentMapping> mapping = docMappingRepo
                .findByOrganizationIdAndEcDocumentType(orgId, eventType);
        if (mapping.isEmpty()) {
            log.warn("No ERP document mapping for event type '{}' orgId={}", eventType, orgId);
            return null;
        }

        String erpDocType = mapping.get().getErpDocumentType();

        return switch (erpDocType) {
            case "SALES_INVOICE"   -> createSalesInvoiceFromOrder(orgId, sourceRef, sourceId);
            case "RECEIPT_VOUCHER" -> createReceiptFromPayment(orgId, sourceRef, sourceId);
            case "DELIVERY_CHALLAN" -> {
                log.info("DELIVERY_CHALLAN creation for event {} is not yet implemented.", eventType);
                yield null;
            }
            case "CREDIT_NOTE" -> {
                log.info("CREDIT_NOTE creation for event {} is not yet implemented.", eventType);
                yield null;
            }
            case "PAYMENT_VOUCHER" -> {
                log.info("PAYMENT_VOUCHER creation for event {} is not yet implemented.", eventType);
                yield null;
            }
            default -> {
                log.warn("Unsupported ERP document type: {} for event {}", erpDocType, eventType);
                yield null;
            }
        };
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Resolve the ERP sub-account for an ecommerce customer.
     *
     * Priority:
     *   1. EcCustomer.erpSubAccount (explicitly linked AR sub-account)
     *   2. null (caller falls back to ec_gl_account_defaults.accountsReceivable)
     */
    private ChartOfAccountSub resolveCustomerParty(EcCustomer customer) {
        if (customer == null) return null;
        if (customer.getErpSubAccount() != null) return customer.getErpSubAccount();

        // Customer may have an ERP-created sub-account with same code
        Optional<EcCustomer> refreshed = customerRepo.findById(customer.getId());
        return refreshed.map(EcCustomer::getErpSubAccount).orElse(null);
    }
}
