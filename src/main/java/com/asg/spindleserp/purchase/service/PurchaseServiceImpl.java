package com.asg.spindleserp.purchase.service;

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
import com.asg.spindleserp.purchase.dto.PurchaseDocumentDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

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

    // ── NEW: needed for full PI → JournalEntry integration ───────────────────
    private final JournalEntryMasterRepository    jemRepo;
    private final ChartOfAccountRepository        coaRepo;

    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");

    // =========================================================================
    // SAVE (CREATE / UPDATE DRAFT)
    // =========================================================================

    @Override
    public PurchaseDocumentDTO save(PurchaseDocumentDTO dto) {
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
    public PurchaseDocumentDTO confirm(Long id) {
        BusinessDocument doc = findDoc(id);
        guardDraft(doc);

        switch (doc.getDocumentType().name()) {
            case "PURCHASE_ORDER"     -> confirmPO(doc);
            case "GOODS_RECEIPT_NOTE" -> confirmGRN(doc);
            case "PURCHASE_INVOICE"   -> confirmInvoice(doc);
            case "DEBIT_NOTE"         -> confirmDebitNote(doc);
            default -> throw new IllegalArgumentException(
                "Unsupported document type: " + doc.getDocumentType().name());
        }

        doc.setStatus("CONFIRMED");
        doc.setUpdatedAt(LocalDateTime.now());
        doc.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        return toDTO(docRepo.save(doc));
    }

    // ── PO: status only, no stock ──────────────────────────────────────────
    private void confirmPO(BusinessDocument doc) {
        log.info("PO {} confirmed.", doc.getDocumentNo());
    }

    // ── GRN: post PURCHASE_RECEIPT stock transactions ──────────────────────
    private void confirmGRN(BusinessDocument doc) {
        if (doc.getWarehouse() == null)
            throw new IllegalStateException("Warehouse is required to confirm GRN.");

        for (BusinessDocumentLine line : doc.getLines()) {
            InventoryLot lot = resolveOrCreateLot(doc, line);
            if (lot != null) line.setInventoryLot(lot);
            postStockTransaction(doc, line, MovementType.PURCHASE_RECEIPT, doc.getWarehouse());
        }
        doc.setStockPosted(true);

        if (doc.getParentDocument() != null) {
            checkAndCloseParentPO(doc.getParentDocument());
        }
    }

    // ── PI: create PURCHASE_VOUCHER (JEM) + update supplier balance ────────
    /**
     * FIX: Previously only updated ChartOfAccountSub.currentBalance — creating
     * no JournalEntryMaster record, which meant Payment Vouchers could not
     * allocate against this invoice (openVouchersForParty returned nothing).
     *
     * Now:
     *  1. Finds the AP control account (SUPPLIER type under ASSET/LIABILITY tree)
     *  2. Finds the PURCHASE/EXPENSE account (from line items or fallback lookup)
     *  3. Creates a PURCHASE_VOUCHER JEM with two lines:
     *       DR  Purchases/Inventory Account   (totalAmount)
     *       CR  Accounts Payable (sub-account = supplier)  (totalAmount)
     *  4. Sets doc.journalEntry = the saved JEM  (FK: global_business_documents has no
     *     direct journal_entry_id, so we store it via reference_no cross-link)
     *  5. Updates sub-account currentBalance (AP increases)
     */
    private void confirmInvoice(BusinessDocument doc) {
        if (doc.getParty() == null)
            throw new IllegalStateException("Supplier (party) is required to confirm Purchase Invoice.");
        if (doc.getTotalAmount() == null || doc.getTotalAmount().compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalStateException("Invoice total amount cannot be zero.");

        Long orgId = doc.getOrganization().getId();
        String username = SecurityHelper.currentUsername().orElse("system");
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);

        // ── Step 1: Resolve AP control account for this supplier ─────────────
        // Look up the SUPPLIER sub-account's main account (Accounts Payable control account)
        var supplierSub = subRepo.findById(doc.getParty().getId())
            .orElseThrow(() -> new IllegalStateException(
                "Supplier account not found: " + doc.getParty().getId()));
        ChartOfAccount apAccount = supplierSub.getMainAccount();
        if (apAccount == null)
            throw new IllegalStateException(
                "Supplier '" + supplierSub.getSubAccountName() +
                "' has no linked main account (AP control account). " +
                "Set the main account on the supplier sub-account first.");

        // ── Step 2: Resolve Purchase/Expense account ─────────────────────────
        // Try to find a PURCHASE account from first item's category, else fallback to
        // an org-level EXPENSE account with code 'PURCH-EXP' or the first EXPENSE account.
        ChartOfAccount purchaseAccount = resolvePurchaseAccount(orgId);
        if (purchaseAccount == null)
            throw new IllegalStateException(
                "No Purchase/Expense account found for this organization. " +
                "Create an account of type EXPENSE and configure it as the default purchase account.");

        // ── Step 3: Build PURCHASE_VOUCHER JEM ───────────────────────────────
        String voucherNo = seqService.nextDocumentNumber(orgId, "PV", year);

        JournalEntryMaster jem = new JournalEntryMaster();
        jem.setOrganization(doc.getOrganization());
        jem.setVoucherType(VoucherType.PURCHASE_VOUCHER);
        jem.setVoucherNo(voucherNo);
        jem.setVoucherDate(doc.getDocumentDate());
        jem.setDueDate(doc.getRequiredDate() != null ? doc.getRequiredDate()
                       : doc.getDocumentDate().plusDays(30));
        jem.setVoucherStatus("POSTED");    // PI confirm = immediately posted to GL
        jem.setPosted(true);
        jem.setReversed(false);
        jem.setTotalAmount(doc.getTotalAmount());
        jem.setTotalDebit(doc.getTotalAmount());
        jem.setTotalCredit(doc.getTotalAmount());
        jem.setAllocatedAmount(BigDecimal.ZERO);
        jem.setPartyId(doc.getParty().getId());
        jem.setPartyType("SUPPLIER");
        jem.setReferenceNo(doc.getDocumentNo());   // links back to PI doc number
        jem.setNarration("Purchase Invoice: " + doc.getDocumentNo()
            + (doc.getReferenceNo() != null ? " / Ref: " + doc.getReferenceNo() : ""));
        jem.setPostedBy(username);
        jem.setPostedAt(LocalDateTime.now());
        jem.setCreatedBy(username);
        jem.setUpdatedBy(username);

        // Line 1: DEBIT Purchases Account
        JournalEntryLine drLine = new JournalEntryLine();
        drLine.setJournalEntry(jem);
        drLine.setLineNumber(1);
        drLine.setAccount(purchaseAccount);
        drLine.setEntryType(JournalEntryLine.EntryType.DEBIT);
        drLine.setAmount(doc.getTotalAmount());
        drLine.setNarration("Purchase: " + doc.getDocumentNo());
        drLine.setOrganization(doc.getOrganization());
        drLine.setTaxLine(false);

        // Line 2: CREDIT Accounts Payable (sub-account = supplier)
        JournalEntryLine crLine = new JournalEntryLine();
        crLine.setJournalEntry(jem);
        crLine.setLineNumber(2);
        crLine.setAccount(apAccount);
        crLine.setSubAccount(supplierSub);
        crLine.setEntryType(JournalEntryLine.EntryType.CREDIT);
        crLine.setAmount(doc.getTotalAmount());
        crLine.setNarration("AP: " + supplierSub.getSubAccountCode()
            + " — " + supplierSub.getSubAccountName());
        crLine.setOrganization(doc.getOrganization());
        crLine.setTaxLine(false);

        jem.getLines().add(drLine);
        jem.getLines().add(crLine);

        JournalEntryMaster savedJem = jemRepo.save(jem);
        log.info("Purchase Invoice {} confirmed. PURCHASE_VOUCHER {} created. Party: {}",
                 doc.getDocumentNo(), savedJem.getVoucherNo(), supplierSub.getSubAccountName());

        // ── Step 4: Update supplier's AP balance ─────────────────────────────
        // currentBalance INCREASES (we owe more to the supplier)
        BigDecimal current = supplierSub.getCurrentBalance() != null
                             ? supplierSub.getCurrentBalance() : BigDecimal.ZERO;
        supplierSub.setCurrentBalance(current.add(doc.getTotalAmount()));
        subRepo.save(supplierSub);

        // ── Step 5: Set invoice due amount = total (nothing paid yet) ─────────
        doc.setPaidAmount(BigDecimal.ZERO);
        doc.setDueAmount(doc.getTotalAmount());
        doc.setAccountingPosted(true);
    }

    /**
     * Looks up the purchase/expense account for this org.
     * Priority:
     *  1. COA with account_code = 'PURCHASE-ACCOUNT' (org-level config)
     *  2. First active EXPENSE account whose code starts with 'PURCH'
     *  3. Any first active EXPENSE account
     */
    private ChartOfAccount resolvePurchaseAccount(Long orgId) {
        // 1. Exact code lookup
        Optional<ChartOfAccount> exact = coaRepo
            .findByOrganizationIdAndAccountCodeIgnoreCase(orgId, "PURCHASE-ACCOUNT");
        if (exact.isPresent()) return exact.get();

        // 2. PURCH* EXPENSE account
        Optional<ChartOfAccount> byPrefix = coaRepo
            .findFirstByOrganizationIdAndAccountTypeAndAccountCodeStartingWithIgnoreCaseAndIsActiveTrue(
                orgId, ChartOfAccount.AccountType.EXPENSE, "PURCH");
        if (byPrefix.isPresent()) return byPrefix.get();

        // 3. First EXPENSE account
        return coaRepo
            .findFirstByOrganizationIdAndAccountTypeAndIsActiveTrue(
                orgId, ChartOfAccount.AccountType.EXPENSE)
            .orElse(null);
    }

    // ── Debit Note: post SUPPLIER_RETURN stock + reduce AP balance ─────────
    private void confirmDebitNote(BusinessDocument doc) {
        if (doc.getWarehouse() == null)
            throw new IllegalStateException("Warehouse is required to confirm Debit Note.");

        for (BusinessDocumentLine line : doc.getLines()) {
            BigDecimal avail = availableQty(line, doc.getWarehouse().getId());
            if (avail.compareTo(line.getQuantity()) < 0)
                throw new IllegalArgumentException(
                    "Insufficient stock to return item '" + line.getItemCode() +
                    "'. Available: " + avail + ", Requested: " + line.getQuantity());
            postStockTransaction(doc, line, MovementType.SUPPLIER_RETURN, doc.getWarehouse());
        }
        doc.setStockPosted(true);

        if (doc.getParty() != null && doc.getTotalAmount() != null) {
            subRepo.findById(doc.getParty().getId()).ifPresent(sub -> {
                BigDecimal current = sub.getCurrentBalance() != null
                                     ? sub.getCurrentBalance() : BigDecimal.ZERO;
                sub.setCurrentBalance(current.subtract(doc.getTotalAmount()));
                subRepo.save(sub);
            });
        }
    }

    // =========================================================================
    // CANCEL
    // =========================================================================

    @Override
    public PurchaseDocumentDTO cancel(Long id) {
        BusinessDocument doc = findDoc(id);
        guardDraft(doc);
        doc.setStatus("CANCELLED");
        doc.setUpdatedAt(LocalDateTime.now());
        return toDTO(docRepo.save(doc));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

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
    public PurchaseDocumentDTO findById(Long id) {
        return toDTO(findDoc(id));
    }

    // =========================================================================
    // POPULATE FROM SOURCE (cascade populate next document)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PurchaseDocumentDTO populateFromSource(Long parentId, String childType) {
        BusinessDocument parent = findDoc(parentId);

        if (!"CONFIRMED".equals(parent.getStatus()))
            throw new IllegalStateException(
                "Source document must be CONFIRMED before creating " + childType + ".");

        PurchaseDocumentDTO child = PurchaseDocumentDTO.builder()
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
            .incoterms(parent.getIncoterms())
            .referenceNo(parent.getDocumentNo())
            .build();

        List<PurchaseDocumentDTO.LineDTO> lines = parent.getLines().stream().map(pl -> {
            PurchaseDocumentDTO.LineDTO line = new PurchaseDocumentDTO.LineDTO();
            line.setSourceLineId(pl.getId());
            line.setItemId(pl.getItem().getId());
            line.setItemCode(pl.getItemCode());
            line.setItemName(pl.getItemName());
            line.setUnitCode(pl.getUnitCode());
            line.setUnitPrice(pl.getUnitPrice());
            line.setDiscountAmount(pl.getDiscountAmount());
            line.setTaxAmount(pl.getTaxAmount());

            switch (childType) {
                case "GOODS_RECEIPT_NOTE" -> {
                    line.setQuantity(pl.getQuantity());
                    line.setReceivedQty(pl.getQuantity());
                    line.setLineAmount(pl.getLineAmount());
                    line.setCreateLot(true);
                }
                case "PURCHASE_INVOICE" -> {
                    BigDecimal qty = pl.getReceivedQty() != null
                                     ? pl.getReceivedQty() : pl.getQuantity();
                    line.setQuantity(qty);
                    line.setLotId(pl.getInventoryLot() != null
                                  ? pl.getInventoryLot().getId() : null);
                    line.setLotNumber(pl.getInventoryLot() != null
                                      ? pl.getInventoryLot().getLotNumber() : null);
                    if (pl.getUnitPrice() != null)
                        line.setLineAmount(qty.multiply(pl.getUnitPrice()));
                }
                case "DEBIT_NOTE" -> {
                    line.setQuantity(pl.getQuantity());
                    line.setLotId(pl.getInventoryLot() != null
                                  ? pl.getInventoryLot().getId() : null);
                    line.setLotNumber(pl.getInventoryLot() != null
                                      ? pl.getInventoryLot().getLotNumber() : null);
                    line.setLineAmount(pl.getLineAmount());
                }
            }
            return line;
        }).collect(Collectors.toList());

        child.setLines(lines);
        return child;
    }

    // =========================================================================
    // NEW: POPULATE PAYMENT VOUCHER FROM CONFIRMED PURCHASE INVOICE
    // =========================================================================

    /**
     * Called by GET /purchase/docs/payment-prefill?invoiceId={id}
     *
     * Returns a pre-filled VoucherDTO ready for the Payment Voucher form.
     *
     * Pre-fills:
     *   voucherType = PAYMENT_VOUCHER
     *   partyId / partyDisplay = supplier from invoice
     *   partyType = SUPPLIER
     *   totalAmount = invoice dueAmount (remaining unpaid)
     *   narration = "Payment against " + invoice.documentNo
     *   allocations[0] = {sourceVoucherId = JEM.id, allocatedAmount = dueAmount}
     *
     * The frontend opens the Payment Voucher form pre-filled with this data.
     * User selects the bank/cash account and confirms the payment amount,
     * then calls POST /accounts/vouchers/save → POST /accounts/vouchers/post/{id}
     */
    @Override
    @Transactional(readOnly = true)
    public VoucherDTO populatePaymentFromInvoice(Long invoiceId) {
        BusinessDocument invoice = findDoc(invoiceId);

        if (!"CONFIRMED".equals(invoice.getStatus()))
            throw new IllegalStateException(
                "Invoice must be CONFIRMED before creating a payment. Status: "
                + invoice.getStatus());
        if (!Boolean.TRUE.equals(invoice.isAccountingPosted()))
            throw new IllegalStateException(
                "Invoice " + invoice.getDocumentNo() +
                " has not been posted to accounts yet. Confirm it first.");

        BigDecimal dueAmount = invoice.getDueAmount() != null
                               ? invoice.getDueAmount() : BigDecimal.ZERO;
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException(
                "Invoice " + invoice.getDocumentNo() +
                " is already fully paid (due amount = 0).");

        // Find the matching PURCHASE_VOUCHER JEM by reference_no = invoice.documentNo
        JournalEntryMaster jem = jemRepo
            .findByOrganizationIdAndReferenceNoAndVoucherType(
                invoice.getOrganization().getId(),
                invoice.getDocumentNo(),
                VoucherType.PURCHASE_VOUCHER)
            .orElseThrow(() -> new IllegalStateException(
                "No accounting voucher found for invoice " + invoice.getDocumentNo() +
                ". Please confirm the invoice again to regenerate the accounting entry."));

        // Build pre-filled VoucherDTO
        VoucherDTO voucherDTO = new VoucherDTO();
        voucherDTO.setVoucherType("PAYMENT_VOUCHER");
        voucherDTO.setVoucherStatus("DRAFT");
        voucherDTO.setVoucherDate(LocalDate.now());
        voucherDTO.setDueDate(invoice.getRequiredDate());
        voucherDTO.setTotalAmount(dueAmount);
        voucherDTO.setPartyType("SUPPLIER");
        voucherDTO.setPartyId(invoice.getParty().getId());
        voucherDTO.setPartyDisplay(invoice.getParty().getSubAccountCode()
            + " — " + invoice.getParty().getSubAccountName());
        voucherDTO.setPartyBalance(invoice.getParty().getCurrentBalance());
        voucherDTO.setReferenceNo(invoice.getDocumentNo());
        voucherDTO.setNarration("Payment against Purchase Invoice: " + invoice.getDocumentNo());

        // Pre-fill allocation against the JEM
        VoucherDTO.AllocationDTO alloc = new VoucherDTO.AllocationDTO();
        alloc.setSourceVoucherId(jem.getId());
        alloc.setSourceVoucherNo(jem.getVoucherNo());
        alloc.setSourceVoucherType(jem.getVoucherType().name());
        alloc.setSourceDueDate(jem.getDueDate());
        alloc.setSourceTotal(jem.getTotalAmount());
        alloc.setSourceAlreadyAllocated(jem.getAllocatedAmount());
        alloc.setSourceRemaining(dueAmount);
        alloc.setAllocatedAmount(dueAmount);   // default = full settlement
        alloc.setDiscountAmount(BigDecimal.ZERO);
        alloc.setWriteOffAmount(BigDecimal.ZERO);
        alloc.setAllocationDate(LocalDate.now());
        alloc.setNarration("Settlement of " + invoice.getDocumentNo());
        voucherDTO.setAllocations(List.of(alloc));

        return voucherDTO;
    }

    // =========================================================================
    // DATATABLE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(String documentType, int draw, int start,
                                            int length, String search) {
        Long   orgId = SecurityHelper.currentOrgId().orElse(null);
        String fn    = jsFnPrefix(documentType);

        String where = "WHERE d.document_type = '" + documentType + "'"
            + " AND d.is_deleted = false"
            + (orgId != null ? " AND d.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "d.document_no", "d.reference_no", "d.document_no_manual",
                "s.sub_account_code", "s.sub_account_name", "d.status"));

        String nextDocAction = nextDocActionButton(documentType, fn);

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY d.id DESC)                              AS sl,
                COUNT(*)     OVER ()                                                AS full_count,
                d.id,
                d.document_no,
                COALESCE(d.document_no_manual, '—')                                AS document_no_manual,
                d.document_type,
                TO_CHAR(d.document_date, 'DD-Mon-YYYY')                             AS document_date,
                COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—')   AS supplier_name,
                COALESCE(w.warehouse_name, '—')                                     AS warehouse_name,
                COALESCE(d.total_amount::text, '—')                                 AS total_amount,
                COALESCE(d.paid_amount::text,  '—')                                 AS paid_amount,
                COALESCE(d.due_amount::text,   '—')                                 AS due_amount,
                COALESCE(d.reference_no, '—')                                       AS reference_no,
                d.stock_posted,
                d.accounting_posted,
                TO_CHAR(d.created_at, 'DD-Mon-YYYY')                                AS created_at,
                COALESCE(d.created_by, '—')                                         AS created_by,
                CASE d.status
                    WHEN 'DRAFT'      THEN '<span class="badge bg-secondary">Draft</span>'
                    WHEN 'CONFIRMED'  THEN '<span class="badge bg-success">Confirmed</span>'
                    WHEN 'CANCELLED'  THEN '<span class="badge bg-danger">Cancelled</span>'
                    WHEN 'CLOSED'     THEN '<span class="badge bg-dark">Closed</span>'
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
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id  = d.party_id
            LEFT   JOIN org_warehouses            w ON w.id  = d.warehouse_id
            %3$s
            ORDER  BY d.id DESC
            OFFSET %4$d LIMIT %5$d
            """, fn, nextDocAction, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // AJAX HELPERS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> openPOsForSupplier(Long supplierId) {
        String sql = """
            SELECT d.id,
                   d.document_no,
                   TO_CHAR(d.document_date, 'DD-Mon-YYYY') AS document_date,
                   COALESCE(d.total_amount, 0)              AS total_amount,
                   d.reference_no,
                   COUNT(l.id)                              AS line_count
            FROM   global_business_documents d
            LEFT   JOIN global_business_document_lines l ON l.document_id = d.id
            WHERE  d.document_type = 'PURCHASE_ORDER'
              AND  d.status        = 'CONFIRMED'
              AND  d.is_deleted    = false
              AND  d.party_id      = ?
            GROUP  BY d.id, d.document_no, d.document_date, d.total_amount, d.reference_no
            ORDER  BY d.document_date DESC
            """;
        return jdbcTemplate.queryForList(sql, supplierId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> confirmedGRNsForSupplier(Long supplierId) {
        String sql = """
            SELECT d.id,
                   d.document_no,
                   TO_CHAR(d.document_date, 'DD-Mon-YYYY') AS document_date,
                   COALESCE(d.total_amount, 0)              AS total_amount,
                   d.reference_no
            FROM   global_business_documents d
            WHERE  d.document_type = 'GOODS_RECEIPT_NOTE'
              AND  d.status        = 'CONFIRMED'
              AND  d.is_deleted    = false
              AND  d.party_id      = ?
            ORDER  BY d.document_date DESC
            """;
        return jdbcTemplate.queryForList(sql, supplierId);
    }

    // =========================================================================
    // MAPPING  entity → DTO
    // =========================================================================

    @Override
    public PurchaseDocumentDTO toDTO(BusinessDocument e) {
        PurchaseDocumentDTO d = PurchaseDocumentDTO.builder()
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
            d.setPartyDisplay(e.getParty().getSubAccountCode() + " — "
                              + e.getParty().getSubAccountName());
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

        List<PurchaseDocumentDTO.LineDTO> lines = e.getLines().stream().map(l -> {
            PurchaseDocumentDTO.LineDTO ld = new PurchaseDocumentDTO.LineDTO();
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
            ld.setReceivedQty(l.getReceivedQty());
            ld.setAcceptedQty(l.getAcceptedQty());
            ld.setRejectedQty(l.getRejectedQty());
            ld.setUnitPrice(l.getUnitPrice());
            ld.setDiscountAmount(l.getDiscountAmount());
            ld.setTaxAmount(l.getTaxAmount());
            ld.setLineAmount(l.getLineAmount());
            ld.setBatchNumber(l.getBatchNumber());
            ld.setExpectedDate(l.getExpectedDate());
            ld.setQualityStatus(l.getQualityStatus());
            ld.setQualityRemarks(l.getQualityRemarks());
            ld.setRemarks(l.getRemarks());
            return ld;
        }).collect(Collectors.toList());
        d.setLines(lines);
        return d;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void buildHeader(PurchaseDocumentDTO dto, BusinessDocument e) {
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
        e.setRequiredDate(dto.getRequiredDate());
        e.setDeliveryDate(dto.getDeliveryDate());
        e.setValidityDate(dto.getValidityDate());
        e.setOtherCharges(dto.getOtherCharges());
        e.setTermsAndConditions(dto.getTermsAndConditions());
        e.setRemarks(dto.getRemarks());

        if (e.getOrganization() == null)
            e.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getPartyId() != null)
            e.setParty(subRepo.getReferenceById(dto.getPartyId()));
        if (dto.getWarehouseId() != null)
            e.setWarehouse(whRepo.getReferenceById(dto.getWarehouseId()));
        if (dto.getParentDocumentId() != null && e.getParentDocument() == null)
            e.setParentDocument(docRepo.getReferenceById(dto.getParentDocumentId()));

        String user = SecurityHelper.currentUsername().orElse("system");
        if (e.getCreatedBy() == null) e.setCreatedBy(user);
        e.setUpdatedBy(user);
        if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        if (e.getDocumentNo() == null || e.getDocumentNo().isBlank()) {
            e.setDocumentNo(seqService.nextDocumentNumber(
                e.getOrganization().getId(),
                e.getDocumentType().getCode() + "-" +
                Objects.requireNonNull(ContextProvider.getOrganizationReference()).getCode(),
                String.valueOf(java.time.Year.now().getValue())));
        }
    }

    private void syncLines(PurchaseDocumentDTO dto, BusinessDocument parent) {
        parent.getLines().clear();
        if (dto.getLines() == null) return;
        int num = 1;
        for (PurchaseDocumentDTO.LineDTO ld : dto.getLines()) {
            if (ld.getItemId() == null) continue;
            var item = itemRepo.getReferenceById(ld.getItemId());

            BusinessDocumentLine line = BusinessDocumentLine.builder()
                .organizationId(parent.getOrganization().getId())
                .document(parent)
                .item(item)
                .lineNumber(num++)
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .unitCode(item.getPurchaseUnitCode())
                .quantity(ld.getQuantity())
                .deliveredQty(ld.getDeliveredQty())
                .receivedQty(ld.getReceivedQty())
                .acceptedQty(ld.getAcceptedQty())
                .rejectedQty(ld.getRejectedQty())
                .unitPrice(ld.getUnitPrice())
                .discountAmount(ld.getDiscountAmount())
                .taxAmount(ld.getTaxAmount())
                .lineAmount(ld.getLineAmount())
                .batchNumber(ld.getBatchNumber())
                .expectedDate(ld.getExpectedDate())
                .qualityStatus(ld.getQualityStatus())
                .qualityRemarks(ld.getQualityRemarks())
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
        BigDecimal qty = isInbound ? line.getQuantity() : line.getQuantity().negate();

        InventoryLot lot   = line.getInventoryLot();
        Long         lotId = lot != null ? lot.getId() : null;

        InventoryStockBalance balance = balanceRepo
            .findByItemIdAndWarehouseIdAndLotId(line.getItem().getId(), warehouse.getId(), lotId)
            .orElseGet(() -> InventoryStockBalance.builder()
                .item(line.getItem())
                .warehouse(warehouse)
                .lot(lot)
                .quantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .build());

        BigDecimal newQty = balance.getQuantity().add(qty);
        if (!isInbound && newQty.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(
                "Negative stock for item '" + line.getItemCode() +
                "' in warehouse '" + warehouse.getWarehouseCode() + "'.");

        balance.setQuantity(newQty);
        balance.setLastTransactionTime(LocalDateTime.now());
        if (isInbound && line.getUnitPrice() != null)
            updateAverageCost(balance, line.getQuantity(), line.getUnitPrice());
        balanceRepo.save(balance);

        InventoryTransaction tx = InventoryTransaction.builder()
            .organizationId(doc.getOrganization().getId())
            .item(line.getItem())
            .warehouse(warehouse)
            .lot(lot)
            .businessDocument(doc)
            .documentType(doc.getDocumentType().name())
            .movementType(movType)
            .transactionDate(doc.getDocumentDate())
            .quantity(line.getQuantity())
            .unitCost(line.getUnitPrice())
            .totalCost(line.getLineAmount())
            .balanceAfter(newQty)
            .remarks(line.getRemarks())
            .build();
        txRepo.save(tx);
    }

    private void updateAverageCost(InventoryStockBalance balance, BigDecimal inQty, BigDecimal inCost) {
        if (inQty.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal oldQty  = balance.getQuantity().subtract(inQty);
        BigDecimal oldCost = balance.getAverageCost() != null ? balance.getAverageCost() : BigDecimal.ZERO;
        BigDecimal newTotal = oldQty.multiply(oldCost).add(inQty.multiply(inCost));
        BigDecimal newQty  = balance.getQuantity();
        if (newQty.compareTo(BigDecimal.ZERO) > 0)
            balance.setAverageCost(newTotal.divide(newQty, 4, RoundingMode.HALF_UP));
    }

    private InventoryLot resolveOrCreateLot(BusinessDocument doc, BusinessDocumentLine line) {
        if (line.getInventoryLot() != null) return line.getInventoryLot();
        String lotNumber = doc.getDocumentNo() + "-L" + line.getLineNumber();
        Long orgId = doc.getOrganization().getId();
        return lotRepo.findByOrganizationIdAndLotNumber(orgId, lotNumber).orElseGet(() -> {
            InventoryLot lot = InventoryLot.builder()
                .organizationId(orgId)
                .item(line.getItem())
                .lotNumber(lotNumber)
                .status(InventoryLot.LotStatus.AVAILABLE)
                .unitCost(line.getUnitPrice())
                .receivedDate(doc.getDocumentDate())
                .supplierId(doc.getParty() != null ? doc.getParty().getId() : null)
                .build();
            return lotRepo.save(lot);
        });
    }

    private void checkAndCloseParentPO(BusinessDocument po) {
        boolean allReceived = po.getLines().stream().allMatch(l -> {
            BigDecimal received = l.getReceivedQty() != null ? l.getReceivedQty() : BigDecimal.ZERO;
            return received.compareTo(l.getQuantity()) >= 0;
        });
        if (allReceived) {
            po.setStatus("CLOSED");
            docRepo.save(po);
        }
    }

    private BigDecimal availableQty(BusinessDocumentLine line, Long warehouseId) {
        Long lotId = line.getInventoryLot() != null ? line.getInventoryLot().getId() : null;
        return balanceRepo.findByItemIdAndWarehouseIdAndLotId(
                line.getItem().getId(), warehouseId, lotId)
            .map(b -> b.getQuantity().subtract(b.getReservedQuantity()))
            .orElse(BigDecimal.ZERO);
    }

    private boolean isInbound(MovementType mt) {
        return mt == MovementType.PURCHASE_RECEIPT
            || mt == MovementType.PRODUCTION_RECEIPT
            || mt == MovementType.TRANSFER_IN
            || mt == MovementType.ADJUSTMENT_IN;
    }

    private BusinessDocument findDoc(Long id) {
        return docRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Document #" + id + " not found."));
    }

    private void guardDraft(BusinessDocument doc) {
        if (!"DRAFT".equals(doc.getStatus()))
            throw new IllegalStateException(
                "Document " + doc.getDocumentNo() + " is " + doc.getStatus() +
                ". Only DRAFT documents can be modified.");
    }

    private String jsFnPrefix(String type) {
        return switch (type) {
            case "PURCHASE_ORDER"     -> "po";
            case "GOODS_RECEIPT_NOTE" -> "grn";
            case "PURCHASE_INVOICE"   -> "pi";
            case "DEBIT_NOTE"         -> "dn";
            default                   -> "doc";
        };
    }

    private String nextDocActionButton(String docType, String fn) {
        return switch (docType) {
            case "PURCHASE_ORDER" ->
                "CASE WHEN d.status = 'CONFIRMED' THEN " +
                "'<a href=\"javascript:;\" onclick=\"createGRNFromPO(' || d.id || ')\" " +
                "class=\"btn btn-white btn-sm\" title=\"Create GRN\">" +
                "<i class=\"fas fa-clipboard-check text-teal\"></i></a>' " +
                "ELSE '''' END";
            case "GOODS_RECEIPT_NOTE" ->
                "CASE WHEN d.status = 'CONFIRMED' THEN " +
                "'<a href=\"javascript:;\" onclick=\"createInvoiceFromGRN(' || d.id || ')\" " +
                "class=\"btn btn-white btn-sm\" title=\"Create Invoice\">" +
                "<i class=\"fas fa-file-invoice text-orange\"></i></a>' " +
                "ELSE '''' END";
            case "PURCHASE_INVOICE" ->
                // FIX: createPaymentFromInvoice is now wired to the real endpoint
                "CASE WHEN d.status = 'CONFIRMED' AND COALESCE(d.due_amount,0) > 0 THEN " +
                "'<a href=\"javascript:;\" onclick=\"createPaymentFromInvoice(' || d.id || ')\" " +
                "class=\"btn btn-white btn-sm\" title=\"Make Payment\">" +
                "<i class=\"fas fa-money-check-dollar text-primary\"></i></a>' " +
                "ELSE '''' END";
            default -> "''";
        };
    }
}
