package com.asg.spindleserp.sales.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.entity.JournalEntryLine;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.accounts.repository.JournalEntryMasterRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.common.enums.MovementType;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.global.entity.*;
import com.asg.spindleserp.global.repository.*;
import com.asg.spindleserp.inventory.repository.ItemRepository;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.organization.repository.WarehouseRepository;
import com.asg.spindleserp.sales.dto.SalesDocumentDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SalesServiceImpl
 *
 * Full sales cycle:
 *   SALES_ORDER → DELIVERY_ORDER → SALES_INVOICE → RECEIPT (via VoucherService)
 *                                                → CREDIT_NOTE (optional return)
 *
 * KEY FIX vs original:
 *   confirmInvoice() now creates a SALES_VOUCHER JournalEntryMaster
 *   (DR Accounts Receivable / CR Sales Revenue) so that Receipt Vouchers can
 *   allocate against it via openVouchersForParty(partyId, "CUSTOMER").
 *
 *   populateReceiptFromInvoice(invoiceId) returns a pre-filled VoucherDTO
 *   for the Receipt Voucher form — called from SalesController.receiptPrefill().
 *
 * Sales-specific vs Purchase mirror:
 *   - Stock OUT on Delivery (SALES_ISSUE), Stock IN on Credit Note (RETURN_FROM_CUSTOMER)
 *   - Party type = CUSTOMER, not SUPPLIER
 *   - SALES_VOUCHER (not PURCHASE_VOUCHER) for GL
 *   - AR increases on invoice confirm, decreases on receipt posting
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final BusinessDocumentRepository      docRepo;
    private final BusinessDocumentLineRepository  lineRepo;
    private final InventoryStockBalanceRepository balanceRepo;
    private final InventoryTransactionRepository  txRepo;
    private final InventoryLotRepository          lotRepo;
    private final ChartOfAccountSubRepository     subRepo;
    private final OrganizationRepository          orgRepo;
    private final WarehouseRepository             whRepo;
    private final ItemRepository                  itemRepo;
    private final DocumentSequenceService         seqService;
    private final JdbcTemplate                    jdbcTemplate;

    // ── NEW: needed for SI → SALES_VOUCHER JEM integration ───────────────────
    private final JournalEntryMasterRepository    jemRepo;
    private final ChartOfAccountRepository        coaRepo;

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public SalesDocumentDTO save(SalesDocumentDTO dto) {
        BusinessDocument entity;
        if (dto.getId() != null) {
            entity = findDoc(dto.getId());
            guardDraft(entity);
        } else {
            entity = new BusinessDocument();
            entity.setStockPosted(false);
            entity.setAccountingPosted(false);
            entity.setDeleted(false);
            entity.setStatus("DRAFT");
        }
        buildHeader(dto, entity);
        syncLines(dto, entity);
        recalcTotals(entity);
        return toDTO(docRepo.save(entity));
    }

    // =========================================================================
    // CONFIRM
    // =========================================================================

    @Override
    public SalesDocumentDTO confirm(Long id) {
        BusinessDocument doc = findDoc(id);
        guardDraft(doc);
        switch (doc.getDocumentType().name()) {
            case "SALES_ORDER"    -> confirmSO(doc);
            case "DELIVERY_ORDER" -> confirmDelivery(doc);
            case "SALES_INVOICE"  -> confirmInvoice(doc);
            case "CREDIT_NOTE"    -> confirmCreditNote(doc);
            default -> throw new IllegalArgumentException(
                "Unsupported sales document type: " + doc.getDocumentType().name());
        }
        doc.setStatus("CONFIRMED");
        doc.setUpdatedAt(LocalDateTime.now());
        doc.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        return toDTO(docRepo.save(doc));
    }

    // ── SO: just confirm, optional stock reservation deferred ─────────────
    private void confirmSO(BusinessDocument doc) {
        log.info("Sales Order {} confirmed.", doc.getDocumentNo());
    }

    // ── Delivery: stock OUT (SALES_ISSUE) ──────────────────────────────────
    private void confirmDelivery(BusinessDocument doc) {
        if (doc.getWarehouse() == null)
            throw new IllegalStateException("Warehouse is required to confirm Delivery Note.");
        for (BusinessDocumentLine line : doc.getLines()) {
            BigDecimal avail = availableQty(line, doc.getWarehouse().getId());
            if (avail.compareTo(line.getQuantity()) < 0)
                throw new IllegalArgumentException(
                    "Insufficient stock for item '" + line.getItemCode() +
                    "'. Available: " + avail + ", Required: " + line.getQuantity());
            postStockTransaction(doc, line, MovementType.SALES_ISSUE, doc.getWarehouse());
        }
        doc.setStockPosted(true);
        if (doc.getParentDocument() != null) checkAndCloseParentSO(doc.getParentDocument());
    }

    // ── Sales Invoice: create SALES_VOUCHER JEM + update customer AR ──────
    /**
     * FIX: Previously only updated ChartOfAccountSub.currentBalance — never
     * creating a JournalEntryMaster, so Receipt Vouchers could not allocate
     * against this invoice (openVouchersForParty returned nothing).
     *
     * Now:
     *  1. Finds AR control account from customer's sub-account (mainAccount)
     *  2. Finds SALES_REVENUE account via resolveRevenueAccount()
     *  3. Creates SALES_VOUCHER JEM, status=POSTED, referenceNo=SI.documentNo:
     *       DR  Accounts Receivable (sub-account = customer)
     *       CR  Sales Revenue account
     *  4. Updates customer.currentBalance += totalAmount (AR increases)
     *  5. Sets doc.paidAmount=0, doc.dueAmount=totalAmount, doc.accountingPosted=true
     */
    private void confirmInvoice(BusinessDocument doc) {
        if (doc.getParty() == null)
            throw new IllegalStateException("Customer (party) is required to confirm Sales Invoice.");
        if (doc.getTotalAmount() == null || doc.getTotalAmount().compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalStateException("Invoice total amount cannot be zero.");

        Long orgId    = doc.getOrganization().getId();
        String user   = SecurityHelper.currentUsername().orElse("system");
        String year   = String.valueOf(LocalDate.now().getYear()).substring(2);

        // ── Step 1: Resolve AR account (customer's main_account_id) ──────────
        var customerSub = subRepo.findById(doc.getParty().getId())
            .orElseThrow(() -> new IllegalStateException(
                "Customer account not found: " + doc.getParty().getId()));
        ChartOfAccount arAccount = customerSub.getMainAccount();
        if (arAccount == null)
            throw new IllegalStateException(
                "Customer '" + customerSub.getSubAccountName() +
                "' has no linked main account (AR control account). " +
                "Set the main account on the customer sub-account first.");

        // ── Step 2: Resolve Sales Revenue account ────────────────────────────
        ChartOfAccount revenueAccount = resolveRevenueAccount(orgId);
        if (revenueAccount == null)
            throw new IllegalStateException(
                "No Sales Revenue account found. Create a REVENUE account with " +
                "code 'SALES-REVENUE' or any REVENUE-type account in this organisation.");

        // ── Step 3: Build SALES_VOUCHER JEM ──────────────────────────────────
        String voucherNo = seqService.nextDocumentNumber(orgId, "RV", year);

        JournalEntryMaster jem = new JournalEntryMaster();
        jem.setOrganization(doc.getOrganization());
        jem.setVoucherType(VoucherType.SALES_VOUCHER);
        jem.setVoucherNo(voucherNo);
        jem.setVoucherDate(doc.getDocumentDate());
        jem.setDueDate(doc.getRequiredDate() != null ? doc.getRequiredDate()
                       : doc.getDocumentDate().plusDays(30));
        jem.setVoucherStatus("POSTED");
        jem.setPosted(true);
        jem.setReversed(false);
        jem.setTotalAmount(doc.getTotalAmount());
        jem.setTotalDebit(doc.getTotalAmount());
        jem.setTotalCredit(doc.getTotalAmount());
        jem.setAllocatedAmount(BigDecimal.ZERO);
        jem.setPartyId(doc.getParty().getId());
        jem.setPartyType("CUSTOMER");
        jem.setReferenceNo(doc.getDocumentNo());   // key link-back to SI docNo
        jem.setNarration("Sales Invoice: " + doc.getDocumentNo()
            + (doc.getReferenceNo() != null ? " / Ref: " + doc.getReferenceNo() : ""));
        jem.setPostedBy(user);
        jem.setPostedAt(LocalDateTime.now());
        jem.setCreatedBy(user);
        jem.setUpdatedBy(user);

        // Line 1: DR Accounts Receivable (sub-account = customer)
        JournalEntryLine drLine = new JournalEntryLine();
        drLine.setJournalEntry(jem);
        drLine.setLineNumber(1);
        drLine.setAccount(arAccount);
        drLine.setSubAccount(customerSub);
        drLine.setEntryType(JournalEntryLine.EntryType.DEBIT);
        drLine.setAmount(doc.getTotalAmount());
        drLine.setNarration("AR: " + customerSub.getSubAccountCode()
            + " — " + customerSub.getSubAccountName());
        drLine.setOrganization(doc.getOrganization());
        drLine.setTaxLine(false);

        // Line 2: CR Sales Revenue
        JournalEntryLine crLine = new JournalEntryLine();
        crLine.setJournalEntry(jem);
        crLine.setLineNumber(2);
        crLine.setAccount(revenueAccount);
        crLine.setEntryType(JournalEntryLine.EntryType.CREDIT);
        crLine.setAmount(doc.getTotalAmount());
        crLine.setNarration("Sales: " + doc.getDocumentNo());
        crLine.setOrganization(doc.getOrganization());
        crLine.setTaxLine(false);

        jem.getLines().add(drLine);
        jem.getLines().add(crLine);

        JournalEntryMaster savedJem = jemRepo.save(jem);
        log.info("Sales Invoice {} confirmed. SALES_VOUCHER {} created. Customer: {}",
                 doc.getDocumentNo(), savedJem.getVoucherNo(), customerSub.getSubAccountName());

        // ── Step 4: Update customer's AR balance ─────────────────────────────
        BigDecimal current = customerSub.getCurrentBalance() != null
                             ? customerSub.getCurrentBalance() : BigDecimal.ZERO;
        customerSub.setCurrentBalance(current.add(doc.getTotalAmount()));
        subRepo.save(customerSub);

        // ── Step 5: Set invoice due amount = total ────────────────────────────
        doc.setPaidAmount(BigDecimal.ZERO);
        doc.setDueAmount(doc.getTotalAmount());
        doc.setAccountingPosted(true);
    }

    /**
     * Resolves the Sales Revenue account for this org.
     * Priority:
     *  1. account_code = 'SALES-REVENUE'  (recommended explicit config)
     *  2. First active REVENUE account whose code starts with 'SALES'
     *  3. Any first active REVENUE account
     */
    private ChartOfAccount resolveRevenueAccount(Long orgId) {
        Optional<ChartOfAccount> exact = coaRepo
            .findByOrganizationIdAndAccountCodeIgnoreCase(orgId, "SALES-REVENUE");
        if (exact.isPresent()) return exact.get();

        Optional<ChartOfAccount> byPrefix = coaRepo
            .findFirstByOrganizationIdAndAccountTypeAndAccountCodeStartingWithIgnoreCaseAndIsActiveTrue(
                orgId, ChartOfAccount.AccountType.REVENUE, "SALES");
        if (byPrefix.isPresent()) return byPrefix.get();

        return coaRepo
            .findFirstByOrganizationIdAndAccountTypeAndIsActiveTrue(
                orgId, ChartOfAccount.AccountType.REVENUE)
            .orElse(null);
    }

    // ── Credit Note: stock IN (RETURN_FROM_CUSTOMER) + reduce AR ──────────
    private void confirmCreditNote(BusinessDocument doc) {
        if (doc.getWarehouse() == null)
            throw new IllegalStateException("Warehouse is required to confirm Credit Note (return).");
        for (BusinessDocumentLine line : doc.getLines()) {
            postStockTransaction(doc, line, MovementType.RETURN_FROM_CUSTOMER, doc.getWarehouse());
        }
        doc.setStockPosted(true);
        if (doc.getParty() != null && doc.getTotalAmount() != null) {
            subRepo.findById(doc.getParty().getId()).ifPresent(sub -> {
                BigDecimal cur = sub.getCurrentBalance() != null
                                 ? sub.getCurrentBalance() : BigDecimal.ZERO;
                sub.setCurrentBalance(cur.subtract(doc.getTotalAmount()));
                subRepo.save(sub);
            });
        }
    }

    // =========================================================================
    // CANCEL / DELETE
    // =========================================================================

    @Override
    public SalesDocumentDTO cancel(Long id) {
        BusinessDocument doc = findDoc(id);
        guardDraft(doc);
        doc.setStatus("CANCELLED");
        doc.setUpdatedAt(LocalDateTime.now());
        return toDTO(docRepo.save(doc));
    }

    @Override
    public void delete(Long id) {
        BusinessDocument doc = findDoc(id);
        if ("CONFIRMED".equals(doc.getStatus()))
            throw new IllegalStateException("Confirmed documents cannot be deleted. Cancel first.");
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        doc.setDeletedBy(SecurityHelper.currentUsername().orElse("system"));
        docRepo.save(doc);
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SalesDocumentDTO findById(Long id) {
        return toDTO(findDoc(id));
    }

    // =========================================================================
    // POPULATE FROM SOURCE (cascade populate next document)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SalesDocumentDTO populateFromSource(Long parentId, String childType) {
        BusinessDocument parent = findDoc(parentId);
        if (!"CONFIRMED".equals(parent.getStatus()))
            throw new IllegalStateException(
                "Source document must be CONFIRMED before creating " + childType + ".");

        SalesDocumentDTO child = SalesDocumentDTO.builder()
            .documentType(childType)
            .documentDate(LocalDate.now())
            .status("DRAFT")
            .parentDocumentId(parent.getId())
            .parentDocumentNo(parent.getDocumentNo())
            .parentDocumentType(parent.getDocumentType().name())
            .partyId(parent.getParty() != null ? parent.getParty().getId() : null)
            .partyDisplay(parent.getParty() != null
                ? parent.getParty().getSubAccountCode() + " — " + parent.getParty().getSubAccountName()
                : null)
            .warehouseId(parent.getWarehouse() != null ? parent.getWarehouse().getId() : null)
            .warehouseDisplay(parent.getWarehouse() != null
                ? parent.getWarehouse().getWarehouseName() : null)
            .currency(parent.getCurrency())
            .exchangeRate(parent.getExchangeRate())
            .customerPoNo(parent.getReferenceNo())
            .referenceNo(parent.getDocumentNo())
            .incoterms(parent.getIncoterms())
            .deliveryAddress(parent.getDeliveryAddress())
            .build();

        List<SalesDocumentDTO.LineDTO> lines = parent.getLines().stream().map(pl -> {
            SalesDocumentDTO.LineDTO line = new SalesDocumentDTO.LineDTO();
            line.setSourceLineId(pl.getId());
            line.setItemId(pl.getItem().getId());
            line.setItemCode(pl.getItemCode());
            line.setItemName(pl.getItemName());
            line.setUnitCode(pl.getUnitCode());
            line.setUnitPrice(pl.getUnitPrice());
            line.setDiscountAmount(pl.getDiscountAmount());
            line.setTaxAmount(pl.getTaxAmount());
            if (pl.getInventoryLot() != null) {
                line.setLotId(pl.getInventoryLot().getId());
                line.setLotNumber(pl.getInventoryLot().getLotNumber());
            }
            switch (childType) {
                case "DELIVERY_ORDER" -> {
                    line.setQuantity(pl.getQuantity());
                    line.setDeliveredQty(pl.getQuantity());
                    line.setLineAmount(pl.getLineAmount());
                }
                case "SALES_INVOICE" -> {
                    BigDecimal qty = pl.getDeliveredQty() != null
                                     ? pl.getDeliveredQty() : pl.getQuantity();
                    line.setQuantity(qty);
                    line.setLineAmount(pl.getUnitPrice() != null
                        ? qty.multiply(pl.getUnitPrice()) : pl.getLineAmount());
                }
                case "CREDIT_NOTE" -> {
                    line.setQuantity(pl.getQuantity());
                    line.setLineAmount(pl.getLineAmount());
                }
            }
            return line;
        }).collect(Collectors.toList());

        child.setLines(lines);
        return child;
    }

    // =========================================================================
    // NEW: POPULATE RECEIPT VOUCHER FROM CONFIRMED SALES INVOICE
    // =========================================================================

    /**
     * Called by GET /sales/docs/receipt-prefill?invoiceId={id}
     *
     * Mirror of PurchaseServiceImpl.populatePaymentFromInvoice().
     *
     * Returns a pre-filled VoucherDTO for the Receipt Voucher form:
     *   voucherType  = RECEIPT_VOUCHER
     *   partyType    = CUSTOMER
     *   partyId      = customer sub-account
     *   totalAmount  = invoice dueAmount
     *   narration    = "Receipt against Sales Invoice: " + SI.documentNo
     *   allocations[0] = { sourceVoucherId = JEM.id, allocatedAmount = dueAmount }
     *
     * The frontend opens /accounts/receipt-vouchers with sessionStorage.rvPrefill.
     */
    @Override
    @Transactional(readOnly = true)
    public VoucherDTO populateReceiptFromInvoice(Long invoiceId) {
        BusinessDocument invoice = findDoc(invoiceId);

        if (!"CONFIRMED".equals(invoice.getStatus()))
            throw new IllegalStateException(
                "Invoice must be CONFIRMED before creating a receipt. Status: " + invoice.getStatus());
        if (!Boolean.TRUE.equals(invoice.isAccountingPosted()))
            throw new IllegalStateException(
                "Invoice " + invoice.getDocumentNo() +
                " has not been posted to accounts yet. Confirm it first.");

        BigDecimal dueAmount = invoice.getDueAmount() != null
                               ? invoice.getDueAmount() : BigDecimal.ZERO;
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException(
                "Invoice " + invoice.getDocumentNo() + " is already fully collected (due = 0).");

        // Find SALES_VOUCHER JEM (referenceNo = SI.documentNo)
        JournalEntryMaster jem = jemRepo
            .findByOrganizationIdAndReferenceNoAndVoucherType(
                invoice.getOrganization().getId(),
                invoice.getDocumentNo(),
                VoucherType.SALES_VOUCHER)
            .orElseThrow(() -> new IllegalStateException(
                "No accounting voucher found for invoice " + invoice.getDocumentNo() +
                ". Please confirm the invoice again to regenerate the accounting entry."));

        VoucherDTO dto = new VoucherDTO();
        dto.setVoucherType("RECEIPT_VOUCHER");
        dto.setVoucherStatus("DRAFT");
        dto.setVoucherDate(LocalDate.now());
        dto.setDueDate(invoice.getRequiredDate());
        dto.setTotalAmount(dueAmount);
        dto.setPartyType("CUSTOMER");
        dto.setPartyId(invoice.getParty().getId());
        dto.setPartyDisplay(invoice.getParty().getSubAccountCode()
            + " — " + invoice.getParty().getSubAccountName());
        dto.setPartyBalance(invoice.getParty().getCurrentBalance());
        dto.setReferenceNo(invoice.getDocumentNo());
        dto.setNarration("Receipt against Sales Invoice: " + invoice.getDocumentNo());

        VoucherDTO.AllocationDTO alloc = new VoucherDTO.AllocationDTO();
        alloc.setSourceVoucherId(jem.getId());
        alloc.setSourceVoucherNo(jem.getVoucherNo());
        alloc.setSourceVoucherType(jem.getVoucherType().name());
        alloc.setSourceDueDate(jem.getDueDate());
        alloc.setSourceTotal(jem.getTotalAmount());
        alloc.setSourceAlreadyAllocated(jem.getAllocatedAmount());
        alloc.setSourceRemaining(dueAmount);
        alloc.setAllocatedAmount(dueAmount);
        alloc.setDiscountAmount(BigDecimal.ZERO);
        alloc.setWriteOffAmount(BigDecimal.ZERO);
        alloc.setAllocationDate(LocalDate.now());
        alloc.setNarration("Settlement of " + invoice.getDocumentNo());
        dto.setAllocations(List.of(alloc));

        return dto;
    }

    // =========================================================================
    // DATATABLE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(String documentType, int draw, int start,
                                            int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String fn  = jsFnPrefix(documentType);

        String where = "WHERE d.document_type = '" + documentType + "'"
            + " AND d.is_deleted = false"
            + (orgId != null ? " AND d.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "d.document_no", "d.reference_no", "d.document_no_manual",
                "s.sub_account_code", "s.sub_account_name", "d.status"));

        String nextDocSql = nextDocActionButton(documentType, fn);

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY d.id DESC)                              AS sl,
                COUNT(*)     OVER ()                                                AS full_count,
                d.id,
                d.document_no,
                COALESCE(d.document_no_manual, '—')                                AS document_no_manual,
                d.document_type,
                TO_CHAR(d.document_date, 'DD-Mon-YYYY')                             AS document_date,
                COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—')   AS customer_name,
                COALESCE(w.warehouse_name, '—')                                     AS warehouse_name,
                COALESCE(d.total_amount::text,  '—')                                AS total_amount,
                COALESCE(d.paid_amount::text,   '—')                                AS paid_amount,
                COALESCE(d.due_amount::text,    '—')                                AS due_amount,
                COALESCE(d.reference_no, '—')                                       AS reference_no,
                COALESCE(pd.document_no, '—')                                       AS parent_doc_no,
                TO_CHAR(d.validity_date, 'DD-Mon-YYYY')                             AS validity_date,
                d.stock_posted,
                d.accounting_posted,
                TO_CHAR(d.created_at, 'DD-Mon-YYYY')                                AS created_at,
                COALESCE(d.created_by, '—')                                         AS created_by,
                CASE d.status
                    WHEN 'DRAFT'     THEN '<span class="badge bg-secondary">Draft</span>'
                    WHEN 'CONFIRMED' THEN '<span class="badge bg-success">Confirmed</span>'
                    WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                    WHEN 'CLOSED'    THEN '<span class="badge bg-dark">Closed</span>'
                    ELSE '<span class="badge bg-info">' || d.status || '</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="%1$sShow('     || d.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || CASE WHEN d.status = 'DRAFT' THEN
                        '<a href="javascript:;" onclick="%1$sEdit('    || d.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                        || '<a href="javascript:;" onclick="%1$sConfirm(' || d.id || ')" class="btn btn-white btn-sm" title="Confirm"><i class="fas fa-check-circle text-primary"></i></a>'
                        || '<a href="javascript:;" onclick="%1$sCancel(' || d.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-secondary"></i></a>'
                        || '<a href="javascript:;" onclick="%1$sDelete(' || d.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                       ELSE '' END
                    || %2$s
                    || '</div>'                                                      AS actions
            FROM   global_business_documents    d
            LEFT   JOIN acc_chart_of_accounts_sub s  ON s.id  = d.party_id
            LEFT   JOIN org_warehouses            w  ON w.id  = d.warehouse_id
            LEFT   JOIN global_business_documents pd ON pd.id = d.parent_document_id
            %3$s
            ORDER  BY d.id DESC
            OFFSET %4$d LIMIT %5$d
            """, fn, nextDocSql, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // AJAX HELPERS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> openSOsForCustomer(Long customerId) {
        String sql = """
            SELECT d.id,
                   d.document_no,
                   TO_CHAR(d.document_date, 'DD-Mon-YYYY') AS document_date,
                   COALESCE(d.total_amount, 0)              AS total_amount,
                   d.reference_no,
                   COUNT(l.id)                              AS line_count
            FROM   global_business_documents d
            LEFT   JOIN global_business_document_lines l ON l.document_id = d.id
            WHERE  d.document_type = 'SALES_ORDER'
              AND  d.status        = 'CONFIRMED'
              AND  d.is_deleted    = false
              AND  d.party_id      = ?
            GROUP  BY d.id, d.document_no, d.document_date, d.total_amount, d.reference_no
            ORDER  BY d.document_date DESC
            """;
        return jdbcTemplate.queryForList(sql, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> confirmedDeliveriesForCustomer(Long customerId) {
        String sql = """
            SELECT d.id,
                   d.document_no,
                   TO_CHAR(d.document_date, 'DD-Mon-YYYY') AS document_date,
                   COALESCE(d.total_amount, 0)              AS total_amount,
                   d.reference_no
            FROM   global_business_documents d
            WHERE  d.document_type = 'DELIVERY_ORDER'
              AND  d.status        = 'CONFIRMED'
              AND  d.is_deleted    = false
              AND  d.party_id      = ?
            ORDER  BY d.document_date DESC
            """;
        return jdbcTemplate.queryForList(sql, customerId);
    }

    // =========================================================================
    // MAPPING  entity → DTO
    // =========================================================================

    @Override
    public SalesDocumentDTO toDTO(BusinessDocument e) {
        SalesDocumentDTO d = SalesDocumentDTO.builder()
            .id(e.getId())
            .documentNo(e.getDocumentNo())
            .documentNoManual(e.getDocumentNoManual())
            .documentType(e.getDocumentType() != null ? e.getDocumentType().name() : null)
            .documentDate(e.getDocumentDate())
            .status(e.getStatus())
            .referenceNo(e.getReferenceNo())
            .currency(e.getCurrency())
            .exchangeRate(e.getExchangeRate())
            .incoterms(e.getIncoterms())
            .portOfLoading(e.getPortOfLoading())
            .portOfDischarge(e.getPortOfDischarge())
            .vesselName(e.getVesselName())
            .blNumber(e.getBlNumber())
            .containerNumber(e.getContainerNumber())
            .challanNo(e.getChallanNo())
            .vehicleNumber(e.getVehicleNumber())
            .driverName(e.getDriverName())
            .deliveryAddress(e.getDeliveryAddress())
            .requiredDate(e.getRequiredDate())
            .deliveryDate(e.getDeliveryDate())
            .validityDate(e.getValidityDate())
            .subtotalAmount(e.getSubtotalAmount())
            .discountAmount(e.getDiscountAmount())
            .taxAmount(e.getTaxAmount())
            .otherCharges(e.getOtherCharges())
            .totalAmount(e.getTotalAmount())
            .paidAmount(e.getPaidAmount())
            .dueAmount(e.getDueAmount())
            .stockPosted(e.isStockPosted())
            .accountingPosted(e.isAccountingPosted())
            .termsAndConditions(e.getTermsAndConditions())
            .remarks(e.getRemarks())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy())
            .updatedBy(e.getUpdatedBy())
            .build();

        if (e.getParty() != null) {
            d.setPartyId(e.getParty().getId());
            d.setPartyDisplay(e.getParty().getSubAccountCode() + " — " + e.getParty().getSubAccountName());
            d.setPartyBalance(e.getParty().getCurrentBalance());
        }
        if (e.getWarehouse() != null) {
            d.setWarehouseId(e.getWarehouse().getId());
            d.setWarehouseDisplay(e.getWarehouse().getWarehouseName());
        }
        if (e.getParentDocument() != null) {
            d.setParentDocumentId(e.getParentDocument().getId());
            d.setParentDocumentNo(e.getParentDocument().getDocumentNo());
            d.setParentDocumentType(e.getParentDocument().getDocumentType() != null
                ? e.getParentDocument().getDocumentType().name() : null);
        }

        List<SalesDocumentDTO.LineDTO> lines = e.getLines().stream().map(l -> {
            SalesDocumentDTO.LineDTO ld = new SalesDocumentDTO.LineDTO();
            ld.setId(l.getId());
            ld.setSourceLineId(l.getSourceLine() != null ? l.getSourceLine().getId() : null);
            ld.setLineNumber(l.getLineNumber());
            if (l.getItem() != null) ld.setItemId(l.getItem().getId());
            ld.setItemCode(l.getItemCode());
            ld.setItemName(l.getItemName());
            ld.setUnitCode(l.getUnitCode());
            if (l.getInventoryLot() != null) {
                ld.setLotId(l.getInventoryLot().getId());
                ld.setLotNumber(l.getInventoryLot().getLotNumber());
            }
            ld.setQuantity(l.getQuantity());
            ld.setDeliveredQty(l.getDeliveredQty());
            ld.setReturnedQty(l.getRejectedQty());
            ld.setUnitPrice(l.getUnitPrice());
            ld.setDiscountAmount(l.getDiscountAmount());
            ld.setTaxAmount(l.getTaxAmount());
            ld.setLineAmount(l.getLineAmount());
            ld.setQualityStatus(l.getQualityStatus());
            ld.setRemarks(l.getRemarks());
            return ld;
        }).collect(Collectors.toList());
        d.setLines(lines);
        return d;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void buildHeader(SalesDocumentDTO dto, BusinessDocument e) {
        Long orgId = ContextProvider.getOrganizationId();
        e.setDocumentType(DocumentType.valueOf(dto.getDocumentType()));
        e.setDocumentDate(dto.getDocumentDate());
        e.setDocumentNoManual(dto.getDocumentNoManual());
        e.setReferenceNo(dto.getReferenceNo());
        e.setCurrency(dto.getCurrency());
        e.setExchangeRate(dto.getExchangeRate());
        e.setIncoterms(dto.getIncoterms());
        e.setPortOfLoading(dto.getPortOfLoading());
        e.setPortOfDischarge(dto.getPortOfDischarge());
        e.setVesselName(dto.getVesselName());
        e.setBlNumber(dto.getBlNumber());
        e.setContainerNumber(dto.getContainerNumber());
        e.setChallanNo(dto.getChallanNo());
        e.setVehicleNumber(dto.getVehicleNumber());
        e.setDriverName(dto.getDriverName());
        e.setDeliveryAddress(dto.getDeliveryAddress());
        e.setRequiredDate(dto.getRequiredDate());
        e.setDeliveryDate(dto.getDeliveryDate());
        e.setValidityDate(dto.getValidityDate());
        e.setOtherCharges(dto.getOtherCharges());
        e.setTermsAndConditions(dto.getTermsAndConditions());
        e.setRemarks(dto.getRemarks());

        if (e.getOrganization() == null) e.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getPartyId()     != null) e.setParty(subRepo.getReferenceById(dto.getPartyId()));
        if (dto.getWarehouseId() != null) e.setWarehouse(whRepo.getReferenceById(dto.getWarehouseId()));
        if (dto.getParentDocumentId() != null && e.getParentDocument() == null)
            e.setParentDocument(docRepo.getReferenceById(dto.getParentDocumentId()));

        String user = SecurityHelper.currentUsername().orElse("system");
        if (e.getCreatedBy() == null) e.setCreatedBy(user);
        e.setUpdatedBy(user);
        if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        if (e.getDocumentNo() == null || e.getDocumentNo().isBlank()) {
            e.setDocumentNo(seqService.nextDocumentNumberByDocType(e.getDocumentType()));
        }
    }

    private void syncLines(SalesDocumentDTO dto, BusinessDocument parent) {
        parent.getLines().clear();
        if (dto.getLines() == null) return;
        int num = 1;
        for (SalesDocumentDTO.LineDTO ld : dto.getLines()) {
            if (ld.getItemId() == null) continue;
            var item = itemRepo.getReferenceById(ld.getItemId());
            BusinessDocumentLine line = BusinessDocumentLine.builder()
                .organizationId(parent.getOrganization().getId())
                .document(parent)
                .item(item)
                .lineNumber(num++)
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .unitCode(item.getSalesUnitCode())
                .quantity(ld.getQuantity())
                .deliveredQty(ld.getDeliveredQty())
                .unitPrice(ld.getUnitPrice())
                .discountAmount(ld.getDiscountAmount())
                .taxAmount(ld.getTaxAmount())
                .lineAmount(ld.getLineAmount())
                .qualityStatus(ld.getQualityStatus())
                .remarks(ld.getRemarks())
                .build();
            if (ld.getSourceLineId() != null)
                line.setSourceLine(lineRepo.getReferenceById(ld.getSourceLineId()));
            if (ld.getLotId() != null)
                line.setInventoryLot(lotRepo.getReferenceById(ld.getLotId()));
            parent.getLines().add(line);
        }
    }

    private void recalcTotals(BusinessDocument doc) {
        BigDecimal subtotal = doc.getLines().stream()
            .map(l -> l.getLineAmount() != null ? l.getLineAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = doc.getDiscountAmount() != null ? doc.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tax      = doc.getLines().stream()
            .map(l -> l.getTaxAmount() != null ? l.getTaxAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal other    = doc.getOtherCharges() != null ? doc.getOtherCharges() : BigDecimal.ZERO;
        doc.setSubtotalAmount(subtotal);
        doc.setTaxAmount(tax);
        BigDecimal total = subtotal.subtract(discount).add(tax).add(other);
        doc.setTotalAmount(total);
        BigDecimal paid = doc.getPaidAmount() != null ? doc.getPaidAmount() : BigDecimal.ZERO;
        doc.setDueAmount(total.subtract(paid));
    }

    private void postStockTransaction(BusinessDocument doc, BusinessDocumentLine line,
                                       MovementType movType,
                                       com.asg.spindleserp.organization.entity.Warehouse warehouse) {
        boolean isInbound = isInbound(movType);
        BigDecimal qty    = isInbound ? line.getQuantity() : line.getQuantity().negate();
        InventoryLot lot  = line.getInventoryLot();
        Long lotId        = lot != null ? lot.getId() : null;

        InventoryStockBalance balance = balanceRepo
            .findByItemIdAndWarehouseIdAndLotId(line.getItem().getId(), warehouse.getId(), lotId)
            .orElseGet(() -> InventoryStockBalance.builder()
                .item(line.getItem()).warehouse(warehouse).lot(lot)
                .quantity(BigDecimal.ZERO).reservedQuantity(BigDecimal.ZERO).build());

        BigDecimal newQty = balance.getQuantity().add(qty);
        if (!isInbound && newQty.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(
                "Negative stock for item '" + line.getItemCode() +
                "' in warehouse '" + warehouse.getWarehouseCode() + "'.");

        balance.setQuantity(newQty);
        balance.setLastTransactionTime(LocalDateTime.now());
        balanceRepo.save(balance);

        txRepo.save(InventoryTransaction.builder()
            .organizationId(doc.getOrganization().getId())
            .item(line.getItem()).warehouse(warehouse).lot(lot)
            .businessDocument(doc).documentType(doc.getDocumentType().name())
            .movementType(movType).transactionDate(doc.getDocumentDate())
            .quantity(line.getQuantity()).unitCost(line.getUnitPrice())
            .totalCost(line.getLineAmount()).balanceAfter(newQty)
            .remarks(line.getRemarks())
            .build());
    }

    private void checkAndCloseParentSO(BusinessDocument so) {
        boolean allDelivered = so.getLines().stream().allMatch(l -> {
            BigDecimal delivered = l.getDeliveredQty() != null ? l.getDeliveredQty() : BigDecimal.ZERO;
            return delivered.compareTo(l.getQuantity()) >= 0;
        });
        if (allDelivered) { so.setStatus("CLOSED"); docRepo.save(so); }
    }

    private BigDecimal availableQty(BusinessDocumentLine line, Long warehouseId) {
        Long lotId = line.getInventoryLot() != null ? line.getInventoryLot().getId() : null;
        return balanceRepo.findByItemIdAndWarehouseIdAndLotId(
                line.getItem().getId(), warehouseId, lotId)
            .map(b -> b.getQuantity().subtract(b.getReservedQuantity()))
            .orElse(BigDecimal.ZERO);
    }

    private boolean isInbound(MovementType mt) {
        return mt == MovementType.RETURN_FROM_CUSTOMER || mt == MovementType.PURCHASE_RECEIPT
            || mt == MovementType.TRANSFER_IN          || mt == MovementType.ADJUSTMENT_IN;
    }

    private BusinessDocument findDoc(Long id) {
        return docRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Document #" + id + " not found."));
    }

    private void guardDraft(BusinessDocument doc) {
        if (!"DRAFT".equals(doc.getStatus()))
            throw new IllegalStateException(
                "Document " + doc.getDocumentNo() + " is " + doc.getStatus() +
                ". Only DRAFT can be modified.");
    }

    private String jsFnPrefix(String type) {
        return switch (type) {
            case "SALES_ORDER"    -> "so";
            case "DELIVERY_ORDER" -> "dn";
            case "SALES_INVOICE"  -> "si";
            case "CREDIT_NOTE"    -> "cn";
            default               -> "doc";
        };
    }

    private String nextDocActionButton(String docType, String fn) {
        return switch (docType) {
            case "SALES_ORDER" ->
                "CASE WHEN d.status = 'CONFIRMED' THEN " +
                "'<a href=\"javascript:;\" onclick=\"createDeliveryFromSO(' || d.id || ')\" " +
                "class=\"btn btn-white btn-sm\" title=\"Create Delivery Note\">" +
                "<i class=\"fas fa-shipping-fast text-teal\"></i></a>' " +
                "ELSE '' END";
            case "DELIVERY_ORDER" ->
                "CASE WHEN d.status = 'CONFIRMED' THEN " +
                "'<a href=\"javascript:;\" onclick=\"createInvoiceFromDelivery(' || d.id || ')\" " +
                "class=\"btn btn-white btn-sm\" title=\"Create Invoice\">" +
                "<i class=\"fas fa-file-invoice-dollar text-orange\"></i></a>' " +
                "ELSE '' END";
            case "SALES_INVOICE" ->
                "CASE WHEN d.status = 'CONFIRMED' AND COALESCE(d.due_amount, 0) > 0 THEN " +
                "'<a href=\"javascript:;\" onclick=\"createReceiptFromInvoice(' || d.id || ')\" " +
                "class=\"btn btn-white btn-sm\" title=\"Collect Payment\">" +
                "<i class=\"fas fa-money-bill-wave text-primary\"></i></a>' " +
                "ELSE '' END";
            default -> "''";
        };
    }
}
