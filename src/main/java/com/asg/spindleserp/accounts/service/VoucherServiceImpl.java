package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.repository.*;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.common.util.CommonUtils;
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
 * VoucherServiceImpl
 *
 * Handles all four voucher types:
 *   JOURNAL_VOUCHER  — free GL entry, must balance DR == CR
 *   PAYMENT_VOUCHER  — AP payment, allocates against open payables
 *   RECEIPT_VOUCHER  — AR receipt, allocates against open receivables
 *   CONTRA_VOUCHER   — bank-to-bank / cash transfer, no party or allocation
 *
 * Key design decisions aligned with the uploaded entity files:
 *  - JournalEntryMaster.getDueAmount() is @Transient — never persisted directly
 *  - allocatedAmount is a plain NUMERIC column, updated via atomic JPQL
 *  - ChartOfAccountSub.subAccountType uses the separate @Column("sub_account_type_enum")
 *    NOT the discriminator column — resolved via getSubAccountType()
 *  - VoucherAllocation extends BaseEntity (org audit fields handled automatically)
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final JournalEntryMasterRepository masterRepo;
    private final ChartOfAccountRepository     coaRepo;
    private final ChartOfAccountSubRepository  subRepo;
    private final VoucherAllocationRepository  allocRepo;
    private final DocumentSequenceService      seqService;
    private final JdbcTemplate                 jdbcTemplate;

    // =========================================================================
    // SAVE (CREATE / UPDATE DRAFT)
    // =========================================================================

    @Override
    public VoucherDTO save(VoucherDTO dto) {
        JournalEntryMaster entity;
        if (dto.getId() != null) {
            entity = findEntityByIdPublic(dto.getId());
            if (!"DRAFT".equals(entity.getVoucherStatus())) {
                throw new IllegalStateException(
                    "Only DRAFT vouchers can be edited. Current status: " + entity.getVoucherStatus());
            }
        } else {
            entity = new JournalEntryMaster();
            entity.setAllocatedAmount(BigDecimal.ZERO);
            entity.setVoucherStatus("DRAFT");
            entity.setPosted(false);
            entity.setReversed(false);
        }
        buildHeader(dto, entity);
        syncLines(dto, entity);
        return toDTO(masterRepo.save(entity));
    }

    // =========================================================================
    // POST
    // =========================================================================

    @Override
    public VoucherDTO post(Long id) {
        JournalEntryMaster entity = findEntityByIdPublic(id);
        if (!"DRAFT".equals(entity.getVoucherStatus())) {
            throw new IllegalStateException(
                "Only DRAFT vouchers can be posted. Current status: " + entity.getVoucherStatus());
        }
        if (entity.getVoucherDate() == null) {
            throw new IllegalStateException("Voucher date is required before posting.");
        }

        String vType = entity.getVoucherType() != null ? entity.getVoucherType().name() : "";

        // Validate GL balance for Journal Voucher
        if ("JOURNAL_VOUCHER".equals(vType)) {
            BigDecimal totalDr = entity.getLines().stream()
                .filter(l -> JournalEntryLine.EntryType.DEBIT == l.getEntryType())
                .map(JournalEntryLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCr = entity.getLines().stream()
                .filter(l -> JournalEntryLine.EntryType.CREDIT == l.getEntryType())
                .map(JournalEntryLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalDr.compareTo(totalCr) != 0) {
                throw new IllegalStateException(
                    "Journal voucher not balanced. DR=" + totalDr + " CR=" + totalCr);
            }
            entity.setTotalDebit(totalDr);
            entity.setTotalCredit(totalCr);
            if (entity.getTotalAmount() == null) {
                entity.setTotalAmount(totalDr);
            }
        }

        // Auto-generate voucher number via DocumentSequenceService
        if (entity.getVoucherNo() == null || entity.getVoucherNo().isBlank()) {
            Long orgId = entity.getOrganization().getId();
            String prefix = voucherPrefix(vType);
            String year   = String.valueOf(LocalDate.now().getYear()).substring(2);
            entity.setVoucherNo(seqService.nextDocumentNumber(orgId, prefix, year));
        }

        // Update lifecycle fields
        entity.setVoucherStatus("POSTED");
        entity.setPosted(true);
        entity.setPostedBy(ContextProvider.getCurrentUsername());
        entity.setPostedAt(LocalDateTime.now());

        // Update sub-account running balance (AP/AR)
        if (entity.getPartyId() != null) {
            adjustSubAccountBalance(entity, false);
        }

        return toDTO(masterRepo.save(entity));
    }

    // =========================================================================
    // PROCESS ALLOCATIONS  (called by VoucherController after posting PV / RV)
    // =========================================================================

    /**
     * Saves VoucherAllocation rows and atomically updates allocatedAmount on each
     * source voucher (invoice/bill). Must be called inside a transaction.
     *
     * Business rules enforced:
     *  1. toApply <= source.getDueAmount()           (can't over-allocate a single invoice)
     *  2. alreadyApplied <= payingVoucher.totalAmount (can't allocate more than payment)
     *  3. DB unique constraint (source_id, paying_id) prevents duplicate pairs
     */
    public void processAllocations(JournalEntryMaster payingVoucher,
                                   List<VoucherDTO.AllocationDTO> allocations) {
        if (allocations == null || allocations.isEmpty()) return;

        BigDecimal payingTotal   = payingVoucher.getTotalAmount() != null
                ? payingVoucher.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalApplied  = BigDecimal.ZERO;

        for (VoucherDTO.AllocationDTO ad : allocations) {
            if (ad.getSourceVoucherId() == null
                    || ad.getAllocatedAmount() == null
                    || ad.getAllocatedAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

            JournalEntryMaster source = findEntityByIdPublic(ad.getSourceVoucherId());
            BigDecimal remaining      = source.getDueAmount(); // @Transient
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Skipping: source voucher {} already fully settled.", source.getVoucherNo());
                continue;
            }

            BigDecimal stillAvailable = payingTotal.subtract(totalApplied);
            if (stillAvailable.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal toApply  = ad.getAllocatedAmount().min(remaining).min(stillAvailable);
            BigDecimal discount = ad.getDiscountAmount()  != null ? ad.getDiscountAmount()  : BigDecimal.ZERO;
            BigDecimal writeOff = ad.getWriteOffAmount()  != null ? ad.getWriteOffAmount()  : BigDecimal.ZERO;

            VoucherAllocation alloc = VoucherAllocation.builder()
                .sourceVoucher(source)
                .sourceVoucherNo(source.getVoucherNo())
                .sourceVoucherType(source.getVoucherType() != null ? source.getVoucherType().name() : null)
                .sourcePartyId(source.getPartyId())
                .sourcePartyType(source.getPartyType())
                .payingVoucher(payingVoucher)
                .payingVoucherNo(payingVoucher.getVoucherNo())
                .payingVoucherType(payingVoucher.getVoucherType() != null ? payingVoucher.getVoucherType().name() : null)
                .allocatedAmount(toApply)
                .discountAmount(discount)
                .writeOffAmount(writeOff)
                .allocationDate(ad.getAllocationDate() != null ? ad.getAllocationDate() : LocalDate.now())
                .narration(ad.getNarration())
                .build();

            allocRepo.save(alloc);

            // Cap total settlement to remaining (prevents floating-point over-settlement)
            BigDecimal totalSettlement = toApply.add(discount).add(writeOff).min(remaining);
            masterRepo.addAllocation(source.getId(), totalSettlement);
            totalApplied = totalApplied.add(toApply);
        }
    }

    // =========================================================================
    // REVERSE
    // =========================================================================

    @Override
    public VoucherDTO reverse(Long id, String reason) {
        JournalEntryMaster original = findEntityByIdPublic(id);
        if (!"POSTED".equals(original.getVoucherStatus())) {
            throw new IllegalStateException("Only POSTED vouchers can be reversed.");
        }
        if (original.isReversed()) {
            throw new IllegalStateException("Voucher " + original.getVoucherNo() + " has already been reversed.");
        }

        // Build the mirror reversal voucher
        JournalEntryMaster mirror = new JournalEntryMaster();
        mirror.setVoucherType(original.getVoucherType());
        mirror.setVoucherDate(LocalDate.now());
        mirror.setNarration("REVERSAL OF " + original.getVoucherNo()
            + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        mirror.setReferenceNo(original.getVoucherNo());
        mirror.setVoucherStatus("POSTED");
        mirror.setPosted(true);
        mirror.setPostedBy(ContextProvider.getCurrentUsername());
        mirror.setPostedAt(LocalDateTime.now());
        mirror.setReversedVoucherId(original.getId());
        mirror.setReversed(false);
        mirror.setTotalDebit(original.getTotalCredit());
        mirror.setTotalCredit(original.getTotalDebit());
        mirror.setTotalAmount(original.getTotalAmount());
        mirror.setAllocatedAmount(BigDecimal.ZERO);
        mirror.setPartyId(original.getPartyId());
        mirror.setPartyType(original.getPartyType());

        // Generate reversal voucher number (e.g. JVR-25-000001)
        Long orgId = original.getOrganization().getId();
        String revPrefix = voucherPrefix(original.getVoucherType() != null ? original.getVoucherType().name() : "JV") + "R";
        String year      = String.valueOf(LocalDate.now().getYear()).substring(2);
        mirror.setVoucherNo(seqService.nextDocumentNumber(orgId, revPrefix, year));

        // Mirror all GL lines (swap DEBIT ↔ CREDIT)
        for (JournalEntryLine orig : original.getLines()) {
            JournalEntryLine rev = new JournalEntryLine();
            rev.setJournalEntry(mirror);
            rev.setAccount(orig.getAccount());
            rev.setSubAccount(orig.getSubAccount());
            rev.setCostCenter(orig.getCostCenter());
            rev.setLineNumber(orig.getLineNumber());
            rev.setEntryType(orig.getEntryType() == JournalEntryLine.EntryType.DEBIT
                ? JournalEntryLine.EntryType.CREDIT
                : JournalEntryLine.EntryType.DEBIT);
            rev.setAmount(orig.getAmount());
            rev.setNarration("REV: " + (orig.getNarration() != null ? orig.getNarration() : ""));
            mirror.getLines().add(rev);
        }

        // Undo sub-account balance
        if (original.getPartyId() != null) {
            adjustSubAccountBalance(original, true);
        }

        // Undo all allocations made BY the original paying voucher
        List<VoucherAllocation> paidAllocs = allocRepo.findByPayingVoucherId(original.getId());
        for (VoucherAllocation alloc : paidAllocs) {
            BigDecimal totalUndo = alloc.getAllocatedAmount()
                .add(alloc.getDiscountAmount())
                .add(alloc.getWriteOffAmount());
            int rows = masterRepo.subtractAllocation(alloc.getSourceVoucher().getId(), totalUndo);
            if (rows == 0) {
                log.warn("subtractAllocation: source {} allocated_amount may be less than {}",
                    alloc.getSourceVoucherNo(), totalUndo);
            }
        }
        allocRepo.deleteByPayingVoucherId(original.getId());

        // Mark original as reversed
        original.setVoucherStatus("REVERSED");
        original.setReversed(true);
        masterRepo.save(original);

        return toDTO(masterRepo.save(mirror));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    public void delete(Long id) {
        JournalEntryMaster entity = findEntityByIdPublic(id);
        if (!"DRAFT".equals(entity.getVoucherStatus())) {
            throw new IllegalStateException("Only DRAFT vouchers can be deleted.");
        }
        masterRepo.delete(entity);
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public VoucherDTO findById(Long id) {
        return toDTO(findEntityByIdPublic(id));
    }

    // =========================================================================
    // DATATABLE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(String voucherType, int draw, int start, int length, String search) {

        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String fn = jsFnPrefix(voucherType);
        String where = "AND j.organization_id = " + orgId
                + (voucherType != null && !voucherType.isBlank() ? " AND j.voucher_type = '" + voucherType + "'" : "")
                + CommonUtils.searchILike(search, Arrays.asList("j.voucher_no", "j.reference_no", "j.narration", "s.sub_account_code", "s.sub_account_name")
        );

        String sql = String.format("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY j.id DESC)                         AS sl,
            COUNT(*)     OVER ()                                           AS full_count,
            j.id,
            COALESCE(j.voucher_no, '—')                                    AS voucher_no,
            j.voucher_type,
            j.voucher_status,
            TO_CHAR(j.voucher_date, 'DD-Mon-YYYY')                         AS voucher_date,
            COALESCE(TO_CHAR(j.due_date, 'DD-Mon-YYYY'), '—')              AS due_date,
            j.party_type,
            COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—') AS party_name,
            COALESCE(j.total_amount, 0)                                    AS total_amount,
            COALESCE(j.allocated_amount, 0)                                AS allocated_amount,
            COALESCE(j.total_amount - j.allocated_amount, 0)               AS due_amount,
            COALESCE(j.reference_no, '—')                                  AS reference_no,
            COALESCE(j.payment_mode, '—')                                  AS payment_mode,
            COALESCE(j.cheque_number, '—')                                 AS cheque_number,
            j.is_reversed,
            COALESCE(j.created_by, '—')                                    AS created_by,
            TO_CHAR(j.created_at, 'DD-Mon-YYYY')                           AS created_at,

            CASE j.voucher_status
                WHEN 'DRAFT' THEN '<span class="badge bg-secondary">Draft</span>'
                WHEN 'POSTED' THEN '<span class="badge bg-success">Posted</span>'
                WHEN 'REVERSED' THEN '<span class="badge bg-warning text-dark">Reversed</span>'
                WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                ELSE '<span class="badge bg-light text-dark">' || j.voucher_status || '</span>'
            END AS status_badge,

            '<div class="btn-group">'
                || '<a href="javascript:;" onclick="%1$sShow(' || j.id || ')" class="btn btn-white btn-sm" title="View">'
                || '<i class="fas fa-eye text-success"></i></a>'

                || CASE
                    WHEN j.voucher_status = 'DRAFT' THEN
                        '<a href="javascript:;" onclick="%1$sEdit(' || j.id || ')" class="btn btn-white btn-sm" title="Edit">'
                        || '<i class="fa-regular fa-pen-to-square text-warning"></i></a>'

                        || '<a href="javascript:;" onclick="%1$sPost(' || j.id || ')" class="btn btn-white btn-sm" title="Post">'
                        || '<i class="fas fa-check-circle text-primary"></i></a>'

                        || '<a href="javascript:;" onclick="%1$sDelete(' || j.id || ')" class="btn btn-white btn-sm" title="Delete">'
                        || '<i class="fa-regular fa-trash-can text-danger"></i></a>'
                    ELSE ''
                END

                || CASE
                    WHEN j.voucher_status = 'POSTED'
                         AND COALESCE(j.is_reversed, FALSE) = FALSE THEN
                        '<a href="javascript:;" onclick="%1$sReverse(' || j.id || ')" class="btn btn-white btn-sm" title="Reverse">'
                        || '<i class="fas fa-rotate-left text-danger"></i></a>'
                    ELSE ''
                END

                || '</div>' AS actions

        FROM acc_journal_entry_master j
        LEFT JOIN acc_chart_of_accounts_sub s ON s.id = j.party_id
        %2$s
        ORDER BY j.id DESC
        OFFSET %3$d LIMIT %4$d
        """,
                fn,      // %1$s
                where,   // %2$s
                start,   // %3$d
                length   // %4$d
        );

        System.out.println("Generated SQL:");
        System.out.println(sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // OPEN VOUCHERS FOR ALLOCATION PICKER
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> openVouchersForParty(Long partyId, String partyType) {
        if (partyId == null) return List.of();
        String sql = """
            SELECT j.id,
                   j.voucher_no,
                   j.voucher_type,
                   TO_CHAR(j.voucher_date, 'DD-Mon-YYYY')  AS voucher_date,
                   COALESCE(TO_CHAR(j.due_date, 'DD-Mon-YYYY'), '—') AS due_date,
                   COALESCE(j.total_amount,     0)          AS total_amount,
                   COALESCE(j.allocated_amount, 0)          AS allocated_amount,
                   COALESCE(j.total_amount - j.allocated_amount, 0)  AS due_amount,
                   CASE WHEN j.due_date IS NULL OR j.due_date >= CURRENT_DATE THEN 0
                        ELSE (CURRENT_DATE - j.due_date)
                   END                                       AS days_overdue,
                   j.reference_no,
                   j.narration
            FROM   acc_journal_entry_master j
            WHERE  j.party_id       = ?
              AND  j.party_type     = ?
              AND  j.voucher_status = 'POSTED'
              AND  j.total_amount   > j.allocated_amount
            ORDER  BY j.due_date ASC NULLS LAST, j.voucher_date ASC
            """;
        return jdbcTemplate.queryForList(sql, partyId, partyType);
    }

    // =========================================================================
    // AGING SUMMARY
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse agingSummary(String partyType, int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        String innerWhere = "WHERE j.voucher_status = 'POSTED'"
            + (orgId != null ? " AND j.organization_id = " + orgId : "")
            + " AND j.party_type = '" + partyType + "'"
            + " AND j.total_amount > j.allocated_amount"
            + CommonUtils.searchILike(search, Arrays.asList(
                "s.sub_account_code", "s.sub_account_name",
                "s.contact_person",   "s.contact_phone"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY agg.total_due DESC) AS sl,
                COUNT(*)     OVER ()                            AS full_count,
                agg.*
            FROM (
                SELECT
                    j.party_id,
                    s.sub_account_code                          AS party_code,
                    s.sub_account_name                          AS party_name,
                    COALESCE(s.contact_person, '—')            AS contact_person,
                    COALESCE(s.contact_phone,  '—')            AS contact_phone,
                    SUM(CASE
                        WHEN j.due_date IS NULL OR j.due_date >= CURRENT_DATE
                        THEN (j.total_amount - j.allocated_amount) ELSE 0
                    END) AS bucket_current,
                    SUM(CASE
                        WHEN j.due_date < CURRENT_DATE
                         AND (CURRENT_DATE - j.due_date) BETWEEN 1 AND 30
                        THEN (j.total_amount - j.allocated_amount) ELSE 0
                    END) AS bucket_030,
                    SUM(CASE
                        WHEN j.due_date < CURRENT_DATE
                         AND (CURRENT_DATE - j.due_date) BETWEEN 31 AND 60
                        THEN (j.total_amount - j.allocated_amount) ELSE 0
                    END) AS bucket_3160,
                    SUM(CASE
                        WHEN j.due_date < CURRENT_DATE
                         AND (CURRENT_DATE - j.due_date) BETWEEN 61 AND 90
                        THEN (j.total_amount - j.allocated_amount) ELSE 0
                    END) AS bucket_6190,
                    SUM(CASE
                        WHEN j.due_date < CURRENT_DATE
                         AND (CURRENT_DATE - j.due_date) > 90
                        THEN (j.total_amount - j.allocated_amount) ELSE 0
                    END) AS bucket_90plus,
                    SUM(j.total_amount - j.allocated_amount)    AS total_due
                FROM  acc_journal_entry_master       j
                JOIN  acc_chart_of_accounts_sub      s ON s.id = j.party_id
                %s
                GROUP BY j.party_id, s.sub_account_code, s.sub_account_name,
                         s.contact_person, s.contact_phone
                HAVING SUM(j.total_amount - j.allocated_amount) > 0
            ) agg
            ORDER BY agg.total_due DESC
            OFFSET %d LIMIT %d
            """, innerWhere, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // AGING DETAIL
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> agingDetail(Long partyId, String partyType) {
        String sql = """
            SELECT j.id,
                   j.voucher_no,
                   j.voucher_type,
                   TO_CHAR(j.voucher_date, 'DD-Mon-YYYY')           AS voucher_date,
                   COALESCE(TO_CHAR(j.due_date, 'DD-Mon-YYYY'), '—') AS due_date,
                   j.total_amount,
                   j.allocated_amount,
                   (j.total_amount - j.allocated_amount)             AS due_amount,
                   CASE WHEN j.due_date IS NULL OR j.due_date >= CURRENT_DATE THEN 0
                        ELSE (CURRENT_DATE - j.due_date)
                   END                                                AS days_overdue,
                   CASE
                       WHEN j.due_date IS NULL OR j.due_date >= CURRENT_DATE THEN 'CURRENT'
                       WHEN (CURRENT_DATE - j.due_date) BETWEEN 1  AND 30    THEN '0-30'
                       WHEN (CURRENT_DATE - j.due_date) BETWEEN 31 AND 60    THEN '31-60'
                       WHEN (CURRENT_DATE - j.due_date) BETWEEN 61 AND 90    THEN '61-90'
                       ELSE '90+'
                   END                                                AS aging_bucket,
                   COALESCE(j.reference_no, '—')                    AS reference_no,
                   COALESCE(j.narration,    '—')                    AS narration
            FROM   acc_journal_entry_master j
            WHERE  j.party_id       = ?
              AND  j.party_type     = ?
              AND  j.voucher_status = 'POSTED'
              AND  j.total_amount   > j.allocated_amount
            ORDER  BY j.due_date ASC NULLS LAST, j.voucher_date ASC
            """;
        return jdbcTemplate.queryForList(sql, partyId, partyType);
    }

    // =========================================================================
    // MAPPING: JournalEntryMaster -> VoucherDTO
    // =========================================================================

    @Override
    public VoucherDTO toDTO(JournalEntryMaster e) {
        VoucherDTO d = VoucherDTO.builder()
            .id(e.getId())
            .voucherNo(e.getVoucherNo())
            .voucherType(e.getVoucherType() != null ? e.getVoucherType().name() : null)
            .voucherDate(e.getVoucherDate())
            .dueDate(e.getDueDate())
            .voucherStatus(e.getVoucherStatus())
            .referenceNo(e.getReferenceNo())
            .narration(e.getNarration())
            .totalDebit(e.getTotalDebit())
            .totalCredit(e.getTotalCredit())
            .totalAmount(e.getTotalAmount())
            .allocatedAmount(e.getAllocatedAmount())
            .dueAmount(e.getDueAmount())         // @Transient computed method
            .partyType(e.getPartyType())
            .partyId(e.getPartyId())
            .bankAccountId(e.getBankAccountId())
            .cashAccountId(e.getCashAccountId())
            .paymentMode(e.getPaymentMode())
            .chequeNumber(e.getChequeNumber())
            .chequeDate(e.getChequeDate())
            .reversedVoucherId(e.getReversedVoucherId())
            .reversed(e.isReversed())
            .postedBy(e.getPostedBy())
            .postedAt(e.getPostedAt() != null ? e.getPostedAt().toString() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy())
            .updatedBy(e.getUpdatedBy())
            .build();

        // Resolve party sub-account display
        if (e.getPartyId() != null) {
            subRepo.findById(e.getPartyId()).ifPresent(s -> {
                d.setPartyDisplay(s.getSubAccountCode() + " — " + s.getSubAccountName());
                d.setPartyBalance(s.getCurrentBalance());
            });
        }
        // Resolve bank account display
        if (e.getBankAccountId() != null) {
            subRepo.findById(e.getBankAccountId()).ifPresent(s ->
                d.setBankAccountDisplay(s.getSubAccountCode() + " — " + s.getSubAccountName()));
        }
        // Resolve cash account display (Contra: toAccount)
        if (e.getCashAccountId() != null) {
            subRepo.findById(e.getCashAccountId()).ifPresent(s ->
                d.setCashAccountDisplay(s.getSubAccountCode() + " — " + s.getSubAccountName()));
        }

        // GL lines
        if (e.getLines() != null && !e.getLines().isEmpty()) {
            d.setLines(e.getLines().stream().map(l -> {
                VoucherDTO.LineDTO ld = new VoucherDTO.LineDTO();
                ld.setId(l.getId());
                ld.setLineNumber(l.getLineNumber());
                ld.setEntryType(l.getEntryType() != null ? l.getEntryType().name() : null);
                ld.setAmount(l.getAmount());
                ld.setNarration(l.getNarration());
                ld.setReferenceNo(l.getReferenceNo());
                ld.setTaxCode(l.getTaxCode());
                ld.setIsTaxLine(l.isTaxLine());
                ld.setCurrencyCode(l.getCurrencyCode());
                ld.setExchangeRate(l.getExchangeRate());
                ld.setBaseAmount(l.getBaseAmount());
                if (l.getAccount() != null) {
                    ld.setAccountId(l.getAccount().getId());
                    ld.setAccountDisplay(
                        l.getAccount().getAccountCode() + " — " + l.getAccount().getAccountName());
                }
                if (l.getSubAccount() != null) {
                    ld.setSubAccountId(l.getSubAccount().getId());
                    ld.setSubAccountDisplay(
                        l.getSubAccount().getSubAccountCode() + " — " + l.getSubAccount().getSubAccountName());
                }
                return ld;
            }).collect(Collectors.toList()));
        }

        // Allocations made by this voucher (paying side)
        List<VoucherAllocation> allocs = allocRepo.findByPayingVoucherId(e.getId());
        if (!allocs.isEmpty()) {
            d.setAllocations(allocs.stream().map(a -> {
                VoucherDTO.AllocationDTO ad = new VoucherDTO.AllocationDTO();
                ad.setId(a.getId());
                JournalEntryMaster src = a.getSourceVoucher();
                ad.setSourceVoucherId(src.getId());
                ad.setSourceVoucherNo(a.getSourceVoucherNo());
                ad.setSourceVoucherType(a.getSourceVoucherType());
                ad.setAllocatedAmount(a.getAllocatedAmount());
                ad.setDiscountAmount(a.getDiscountAmount());
                ad.setWriteOffAmount(a.getWriteOffAmount());
                ad.setAllocationDate(a.getAllocationDate());
                ad.setNarration(a.getNarration());
                ad.setSourceDueDate(src.getDueDate());
                ad.setSourceTotal(src.getTotalAmount());
                ad.setSourceAlreadyAllocated(src.getAllocatedAmount());
                ad.setSourceRemaining(src.getDueAmount()); // @Transient
                return ad;
            }).collect(Collectors.toList()));
        }

        return d;
    }

    // =========================================================================
    // PUBLIC ACCESSOR (VoucherController calls this directly)
    // =========================================================================

    /** Package-accessible — avoids unnecessary re-query in controller post flow */
    public JournalEntryMaster findEntityByIdPublic(Long id) {
        return masterRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Voucher #" + id + " not found."));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void buildHeader(VoucherDTO dto, JournalEntryMaster e) {
        e.setVoucherType(VoucherType.valueOf(dto.getVoucherType()));
        e.setVoucherDate(dto.getVoucherDate());
        e.setDueDate(dto.getDueDate());
        e.setNarration(dto.getNarration());
        e.setReferenceNo(dto.getReferenceNo());

        e.setPartyType(dto.getPartyType());
        e.setPartyId(dto.getPartyId());
        e.setPaymentMode(dto.getPaymentMode());
        e.setChequeNumber(dto.getChequeNumber());
        e.setChequeDate(dto.getChequeDate());

        // CONTRA_VOUCHER: fromAccount (DR) → bankAccountId, toAccount (CR) → cashAccountId
        if ("CONTRA_VOUCHER".equals(dto.getVoucherType())) {
            e.setBankAccountId(dto.getFromAccountId());
            e.setCashAccountId(dto.getToAccountId());
        } else {
            e.setBankAccountId(dto.getBankAccountId());
            e.setCashAccountId(dto.getCashAccountId());
        }

        // Derive totalAmount from the DTO or sum the DR lines
        BigDecimal total = dto.getTotalAmount();
        if ((total == null || total.compareTo(BigDecimal.ZERO) == 0)
                && dto.getLines() != null && !dto.getLines().isEmpty()) {
            total = dto.getLines().stream()
                .filter(l -> "DEBIT".equals(l.getEntryType()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        e.setTotalAmount(total);
    }

    private void syncLines(VoucherDTO dto, JournalEntryMaster parent) {
        parent.getLines().clear();
        if (dto.getLines() == null || dto.getLines().isEmpty()) return;
        int num = 1;
        for (VoucherDTO.LineDTO ld : dto.getLines()) {
            if (ld.getAccountId() == null || ld.getEntryType() == null) continue;
            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(parent);
            line.setLineNumber(num++);
            line.setAccount(coaRepo.getReferenceById(ld.getAccountId()));
            if (ld.getSubAccountId() != null)
                line.setSubAccount(subRepo.getReferenceById(ld.getSubAccountId()));
            line.setEntryType(JournalEntryLine.EntryType.valueOf(ld.getEntryType()));
            line.setAmount(ld.getAmount());
            line.setNarration(ld.getNarration());
            line.setReferenceNo(ld.getReferenceNo());
            line.setTaxCode(ld.getTaxCode());
            line.setTaxLine(Boolean.TRUE.equals(ld.getIsTaxLine()));
            line.setCurrencyCode(ld.getCurrencyCode());
            line.setExchangeRate(ld.getExchangeRate());
            line.setBaseAmount(ld.getBaseAmount());
            parent.getLines().add(line);
        }
    }

    /**
     * Adjusts the running currentBalance on the party's ChartOfAccountSub.
     *
     * Balance convention (AP/AR):
     *   Positive balance = OWES US (AR) or WE OWE (AP)
     *   PAYMENT_VOUCHER posted  → party's liability decreases  (we paid them)
     *   RECEIPT_VOUCHER posted  → party's receivable decreases (they paid us)
     *   Other voucher types     → party's balance increases (invoice created)
     *
     * @param entity the voucher being posted or reversed
     * @param undo   true during reversal — apply opposite direction
     */
    private void adjustSubAccountBalance(JournalEntryMaster entity, boolean undo) {
        if (entity.getPartyId() == null) return;
        subRepo.findById(entity.getPartyId()).ifPresent(sub -> {
            BigDecimal current = sub.getCurrentBalance() != null ? sub.getCurrentBalance() : BigDecimal.ZERO;
            BigDecimal amount  = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
            String type = entity.getVoucherType() != null ? entity.getVoucherType().name() : "";

            // Payments and receipts reduce the outstanding balance; invoices increase it
            boolean reduces = "PAYMENT_VOUCHER".equals(type) || "RECEIPT_VOUCHER".equals(type);

            if ((reduces && !undo) || (!reduces && undo)) {
                sub.setCurrentBalance(current.subtract(amount));
            } else {
                sub.setCurrentBalance(current.add(amount));
            }
            subRepo.save(sub);
        });
    }

    private String voucherPrefix(String type) {
        return switch (type) {
            case "JOURNAL_VOUCHER"  -> "JV";
            case "PAYMENT_VOUCHER"  -> "PV";
            case "RECEIPT_VOUCHER"  -> "RV";
            case "CONTRA_VOUCHER"   -> "CV";
            case "DEBIT_NOTE"       -> "DN";
            case "CREDIT_NOTE"      -> "CN";
            default                 -> "VCH";
        };
    }

    private String jsFnPrefix(String type) {
        if (type == null || type.isBlank()) return "vch";
        return switch (type) {
            case "JOURNAL_VOUCHER"  -> "jv";
            case "PAYMENT_VOUCHER"  -> "pv";
            case "RECEIPT_VOUCHER"  -> "rv";
            case "CONTRA_VOUCHER"   -> "cv";
            default                 -> "vch";
        };
    }
}
