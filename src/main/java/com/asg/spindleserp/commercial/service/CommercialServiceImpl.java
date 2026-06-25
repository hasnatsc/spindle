package com.asg.spindleserp.commercial.service;

import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.commercial.dto.*;
import com.asg.spindleserp.commercial.entity.*;
import com.asg.spindleserp.commercial.repository.*;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.inventory.repository.ItemRepository;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
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
public class CommercialServiceImpl implements CommercialService {

    private final CommercialInvoiceRepository  invoiceRepo;
    private final CommercialInvoiceItemRepository itemRepo;
    private final DocumentTermRepository       termRepo;
    private final LcSettlementRepository       settlementRepo;

    private final ChartOfAccountSubRepository  subAccRepo;
    private final ItemRepository               itemMasterRepo;
    private final OrganizationRepository       orgRepo;
    private final DocumentSequenceService      seqService;
    private final JdbcTemplate                 jdbcTemplate;

    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");

    // =========================================================================
    // COMMERCIAL INVOICE
    // =========================================================================

    @Override
    public CommercialInvoiceDTO createInvoice(CommercialInvoiceDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        // Auto-number: ECI-{YY}-{NNNNNN} for export, ICI-{YY}-{NNNNNN} for import
        String prefix = "EXPORT".equals(dto.getInvoiceType()) ? "ECI" : "ICI";
        String no = seqService.nextDocumentNumber(orgId, prefix, LocalDate.now().format(YY));
        CommercialInvoice inv = buildInvoice(dto, new CommercialInvoice());
        inv.setInvoiceNo(no);
        inv.setOrganizationId(orgId);
        inv.setStatus(CommercialInvoice.InvoiceStatus.DRAFT);
        CommercialInvoice saved = invoiceRepo.save(inv);
        syncItems(dto.getItems(), saved);
        syncTerms(dto.getTerms(), saved);
        recalcTotals(saved);
        return toDTO(saved);
    }

    @Override
    public CommercialInvoiceDTO updateInvoice(Long id, CommercialInvoiceDTO dto) {
        CommercialInvoice inv = findInvoice(id);
        guardDraft(inv);
        buildInvoice(dto, inv);
        CommercialInvoice saved = invoiceRepo.save(inv);
        syncItems(dto.getItems(), saved);
        syncTerms(dto.getTerms(), saved);
        recalcTotals(saved);
        return toDTO(saved);
    }

    @Override @Transactional(readOnly = true)
    public CommercialInvoiceDTO findInvoiceById(Long id) { return toDTO(findInvoice(id)); }

    @Override
    public void deleteInvoice(Long id) {
        CommercialInvoice inv = findInvoice(id);
        guardDraft(inv);
        invoiceRepo.delete(inv);
    }

    @Override
    public CommercialInvoiceDTO finalizeInvoice(Long id) {
        CommercialInvoice inv = findInvoice(id);
        if (inv.getStatus() != CommercialInvoice.InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT invoices can be finalized.");
        inv.setStatus(CommercialInvoice.InvoiceStatus.FINALIZED);
        return toDTO(invoiceRepo.save(inv));
    }

    @Override
    public CommercialInvoiceDTO postInvoice(Long id) {
        CommercialInvoice inv = findInvoice(id);
        if (inv.getStatus() != CommercialInvoice.InvoiceStatus.FINALIZED)
            throw new IllegalStateException("Only FINALIZED invoices can be posted.");
        inv.setStatus(CommercialInvoice.InvoiceStatus.POSTED);
        return toDTO(invoiceRepo.save(inv));
    }

    @Override
    public CommercialInvoiceDTO cancelInvoice(Long id, String remarks) {
        CommercialInvoice inv = findInvoice(id);
        if (inv.getStatus() == CommercialInvoice.InvoiceStatus.POSTED)
            throw new IllegalStateException("Posted invoices cannot be cancelled.");
        inv.setStatus(CommercialInvoice.InvoiceStatus.CANCELLED);
        inv.setRemarks((inv.getRemarks() != null ? inv.getRemarks()+"\n" : "") + "[CANCELLED] " + remarks);
        return toDTO(invoiceRepo.save(inv));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse invoiceDatatable(int draw, int start, int length, String search, String invoiceType) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND ci.organization_id=" + orgId : "")
            + (invoiceType != null && !invoiceType.isBlank() ? " AND ci.invoice_type='" + invoiceType + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "ci.invoice_no","ci.currency","ci.incoterms","ci.bl_number","ci.vessel_name"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY ci.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   ci.id, ci.invoice_no, ci.invoice_type, ci.invoice_date,
                   ci.status, ci.currency,
                   COALESCE(ci.exchange_rate::text,'—') AS exchange_rate,
                   COALESCE(ci.total_amount::text,'0.00') AS total_amount,
                   COALESCE(ci.total_amount_bdt::text,'0.00') AS total_amount_bdt,
                   COALESCE(lc.sub_account_code||' — '||lc.sub_account_name,'—') AS lc_name,
                   COALESCE(pt.sub_account_code||' — '||pt.sub_account_name,'—') AS party_name,
                   COALESCE(ci.incoterms,'—') AS incoterms,
                   COALESCE(ci.bl_number,'—') AS bl_number,
                   COALESCE(ci.vessel_name,'—') AS vessel_name,
                   COALESCE(ci.port_of_loading,'—') AS port_of_loading,
                   COALESCE(ci.port_of_discharge,'—') AS port_of_discharge,
                   (SELECT COUNT(*) FROM com_commercial_invoice_item cii WHERE cii.invoice_id=ci.id) AS item_count,
                   CASE ci.status
                       WHEN 'DRAFT'      THEN '<span class="badge bg-secondary">Draft</span>'
                       WHEN 'FINALIZED'  THEN '<span class="badge bg-primary">Finalized</span>'
                       WHEN 'POSTED'     THEN '<span class="badge bg-success">Posted</span>'
                       WHEN 'CANCELLED'  THEN '<span class="badge bg-danger">Cancelled</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="ciShow('     || ci.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN ci.status = 'DRAFT' THEN
                       '<a href="javascript:;" onclick="ciEdit('     || ci.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                       || '<a href="javascript:;" onclick="ciFinalize('|| ci.id || ')" class="btn btn-white btn-sm" title="Finalize"><i class="fas fa-check text-teal"></i></a>'
                       || '<a href="javascript:;" onclick="ciDelete(' || ci.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                      END
                   || CASE WHEN ci.status = 'FINALIZED' THEN
                       '<a href="javascript:;" onclick="ciPost('     || ci.id || ')" class="btn btn-white btn-sm" title="Post"><i class="fas fa-paper-plane text-primary"></i></a>'
                       || '<a href="javascript:;" onclick="ciCancel('|| ci.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-danger"></i></a>'
                      END
                   || '</div>' AS actions
            FROM com_commercial_invoice ci
            LEFT JOIN acc_chart_of_accounts_sub lc ON lc.id = ci.lc_id
            LEFT JOIN acc_chart_of_accounts_sub pt ON pt.id = ci.party_id
            %s ORDER BY ci.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchInvoices(String q, String invoiceType, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page-1)*sz;
        String sql = "SELECT id, invoice_no, invoice_type FROM com_commercial_invoice WHERE status NOT IN ('CANCELLED')"
            + (orgId != null ? " AND organization_id=" + orgId : "")
            + (invoiceType != null && !invoiceType.isBlank() ? " AND invoice_type='" + invoiceType + "'" : "")
            + (q != null && !q.isBlank() ? " AND invoice_no ILIKE '%" + q.replace("'","''") + "%'" : "")
            + " ORDER BY id DESC LIMIT " + (sz+1) + " OFFSET " + off;
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String,Object>> items = rows.stream().limit(sz).map(r ->
            Map.of("id",r.get("id"),"text",r.get("invoice_no")+" ("+r.get("invoice_type")+")")).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public CommercialInvoiceDTO toDTO(CommercialInvoice e) {
        CommercialInvoiceDTO d = CommercialInvoiceDTO.builder()
            .id(e.getId()).invoiceNo(e.getInvoiceNo())
            .invoiceType(e.getInvoiceType() != null ? e.getInvoiceType().name() : "EXPORT")
            .status(e.getStatus() != null ? e.getStatus().name() : "DRAFT")
            .invoiceDate(e.getInvoiceDate()).currency(e.getCurrency())
            .exchangeRate(e.getExchangeRate()).totalAmount(e.getTotalAmount()).totalAmountBdt(e.getTotalAmountBdt())
            .incoterms(e.getIncoterms()).portOfLoading(e.getPortOfLoading()).portOfDischarge(e.getPortOfDischarge())
            .vesselName(e.getVesselName()).blNumber(e.getBlNumber()).containerNo(e.getContainerNo())
            .deliveryId(e.getDeliveryId()).grnId(e.getGrnId()).remarks(e.getRemarks())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .build();
        if (e.getLc()    != null) { d.setLcId(e.getLc().getId()); d.setLcDisplay(e.getLc().getSubAccountCode()+" — "+e.getLc().getSubAccountName()); }
        if (e.getParty() != null) { d.setPartyId(e.getParty().getId()); d.setPartyDisplay(e.getParty().getSubAccountCode()+" — "+e.getParty().getSubAccountName()); }
        // Items
        List<CommercialInvoiceItem> items = itemRepo.findByInvoiceId(e.getId());
        d.setItems(items.stream().map(it -> CommercialInvoiceDTO.ItemLineDTO.builder()
            .id(it.getId()).itemId(it.getItem().getId())
            .itemDisplay(it.getItem().getItemCode()+" — "+it.getItem().getItemName())
            .unit(it.getUnit()).description(it.getDescription())
            .quantity(it.getQuantity()).unitPrice(it.getUnitPrice()).totalAmount(it.getTotalAmount())
            .deliveryDetailId(it.getDeliveryDetailId())
            .build()).collect(Collectors.toList()));
        // Terms
        List<DocumentTerm> terms = termRepo.findByInvoiceId(e.getId());
        d.setTerms(terms.stream().map(t -> CommercialInvoiceDTO.TermLineDTO.builder()
            .id(t.getId()).globalTermsId(t.getGlobalTermsId())
            .title(t.getTitle()).description(t.getDescription()).sortOrder(t.getSortOrder())
            .build()).collect(Collectors.toList()));
        return d;
    }

    // =========================================================================
    // LC SETTLEMENT
    // =========================================================================

    @Override
    public LcSettlementDTO createSettlement(LcSettlementDTO dto) {
        LcSettlement s = buildSettlement(dto, new LcSettlement());
        s.setStatus(LcSettlement.SettlementStatus.PENDING);
        return toDTO(settlementRepo.save(s));
    }

    @Override
    public LcSettlementDTO updateSettlement(Long id, LcSettlementDTO dto) {
        LcSettlement s = findSettlement(id);
        if (s.getStatus() == LcSettlement.SettlementStatus.SETTLED ||
            s.getStatus() == LcSettlement.SettlementStatus.REVERSED)
            throw new IllegalStateException("Settled or reversed settlements cannot be edited.");
        buildSettlement(dto, s);
        return toDTO(settlementRepo.save(s));
    }

    @Override @Transactional(readOnly = true)
    public LcSettlementDTO findSettlementById(Long id) { return toDTO(findSettlement(id)); }

    @Override
    public void deleteSettlement(Long id) {
        LcSettlement s = findSettlement(id);
        if (s.getStatus() != LcSettlement.SettlementStatus.PENDING)
            throw new IllegalStateException("Only PENDING settlements can be deleted.");
        settlementRepo.delete(s);
    }

    @Override
    public LcSettlementDTO settleSettlement(Long id) {
        LcSettlement s = findSettlement(id);
        if (s.getStatus() == LcSettlement.SettlementStatus.SETTLED)
            throw new IllegalStateException("Already settled.");
        s.setStatus(LcSettlement.SettlementStatus.SETTLED);
        return toDTO(settlementRepo.save(s));
    }

    @Override
    public LcSettlementDTO reverseSettlement(Long id) {
        LcSettlement s = findSettlement(id);
        if (s.getStatus() != LcSettlement.SettlementStatus.SETTLED)
            throw new IllegalStateException("Only SETTLED settlements can be reversed.");
        s.setStatus(LcSettlement.SettlementStatus.REVERSED);
        return toDTO(settlementRepo.save(s));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse settlementDatatable(int draw, int start, int length, String search, Long lcId) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        // Settlement has no org_id column; filter via LC → org join
        String where = "WHERE 1=1"
            + (lcId != null ? " AND s.lc_id=" + lcId : "")
            + CommonUtils.searchILike(search, Arrays.asList("lc.sub_account_code","lc.sub_account_name","s.settlement_type","s.status"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY s.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   s.id, s.settlement_date, s.settlement_type, s.status,
                   COALESCE(lc.sub_account_code||' — '||lc.sub_account_name,'—') AS lc_name,
                   COALESCE(s.exchange_rate::text,'—') AS exchange_rate,
                   COALESCE(s.amount_usd::text,'0.00') AS amount_usd,
                   COALESCE(s.amount_bdt::text,'0.00') AS amount_bdt,
                   COALESCE(s.charges::text,'0.00')    AS charges,
                   COALESCE(s.commission::text,'0.00') AS commission,
                   COALESCE(s.interest::text,'0.00')   AS interest,
                   COALESCE(s.loan_amount::text,'0.00')AS loan_amount,
                   CASE s.status
                       WHEN 'PENDING'  THEN '<span class="badge bg-warning text-dark">Pending</span>'
                       WHEN 'PARTIAL'  THEN '<span class="badge bg-info text-dark">Partial</span>'
                       WHEN 'SETTLED'  THEN '<span class="badge bg-success">Settled</span>'
                       WHEN 'REVERSED' THEN '<span class="badge bg-danger">Reversed</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="stlShow('   || s.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN s.status IN ('PENDING','PARTIAL') THEN
                       '<a href="javascript:;" onclick="stlEdit('   || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                       || '<a href="javascript:;" onclick="stlSettle('|| s.id || ')" class="btn btn-white btn-sm" title="Mark Settled"><i class="fas fa-check-double text-success"></i></a>'
                       || '<a href="javascript:;" onclick="stlDelete('|| s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                      END
                   || CASE WHEN s.status = 'SETTLED' THEN
                       '<a href="javascript:;" onclick="stlReverse('|| s.id || ')" class="btn btn-white btn-sm" title="Reverse"><i class="fas fa-undo text-danger"></i></a>'
                      END
                   || '</div>' AS actions
            FROM com_lc_settlement s
            LEFT JOIN acc_chart_of_accounts_sub lc ON lc.id = s.lc_id
            %s ORDER BY s.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public LcSettlementDTO toDTO(LcSettlement e) {
        LcSettlementDTO d = LcSettlementDTO.builder()
            .id(e.getId()).settlementDate(e.getSettlementDate())
            .settlementType(e.getSettlementType() != null ? e.getSettlementType().name() : null)
            .status(e.getStatus() != null ? e.getStatus().name() : "PENDING")
            .exchangeRate(e.getExchangeRate()).amountUsd(e.getAmountUsd()).amountBdt(e.getAmountBdt())
            .marginUsed(e.getMarginUsed()).charges(e.getCharges()).commission(e.getCommission())
            .interest(e.getInterest()).loanAmount(e.getLoanAmount())
            .build();
        if (e.getLc() != null) { d.setLcId(e.getLc().getId()); d.setLcDisplay(e.getLc().getSubAccountCode()+" — "+e.getLc().getSubAccountName()); }
        if (e.getDocument() != null) { d.setDocumentId(e.getDocument().getId()); d.setDocumentDisplay(e.getDocument().getDocumentNo()); }
        return d;
    }

    // =========================================================================
    // DASHBOARD
    // =========================================================================

// =========================================================================
// DASHBOARD SUMMARY — replace existing dashboardSummary() in CommercialServiceImpl
// =========================================================================
// Uses indexes on: com_commercial_invoice (organization_id, invoice_type, status)
//                  com_lc_settlement (lc_id, status)
//                  acc_chart_of_accounts_sub (sub_account_type, organization_id)

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long   orgId    = SecurityHelper.currentOrgId().orElse(null);
        String f        = orgId != null ? " AND organization_id = " + orgId : "";
        String fCI      = orgId != null ? " AND ci.organization_id = " + orgId : "";
        String fS       = orgId != null ? " AND s.organization_id = " + orgId : "";
        String today    = LocalDate.now().toString();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();
        String yearStart= LocalDate.now().withDayOfYear(1).toString();

        Map<String, Object> m = new LinkedHashMap<>();

        // ── 1. Export invoice KPIs ─────────────────────────────────────────────
        String exportSql = """
        SELECT
          COUNT(*) FILTER (WHERE status='DRAFT')     AS draft,
          COUNT(*) FILTER (WHERE status='FINALIZED') AS finalized,
          COUNT(*) FILTER (WHERE status='POSTED')    AS posted,
          COUNT(*) FILTER (WHERE status='CANCELLED') AS cancelled,
          COALESCE(SUM(total_amount)     FILTER (WHERE status='POSTED'), 0) AS posted_usd,
          COALESCE(SUM(total_amount_bdt) FILTER (WHERE status='POSTED'), 0) AS posted_bdt,
          COALESCE(SUM(total_amount)     FILTER (WHERE status='POSTED'
                AND invoice_date >= ?::date), 0) AS posted_mtd_usd,
          COUNT(*)                                    AS total
        FROM com_commercial_invoice
        WHERE invoice_type='EXPORT'""" + f;
        List<Map<String, Object>> expRows = jdbcTemplate.queryForList(exportSql, mtdStart);
        if (!expRows.isEmpty()) {
            Map<String, Object> r = expRows.get(0);
            m.put("exportDraft",     toLong(r, "draft"));
            m.put("exportFinalized", toLong(r, "finalized"));
            m.put("exportPosted",    toLong(r, "posted"));
            m.put("exportCancelled", toLong(r, "cancelled"));
            m.put("exportTotal",     toLong(r, "total"));
            m.put("exportPostedUSD", toBD(r,   "posted_usd"));
            m.put("exportPostedBDT", toBD(r,   "posted_bdt"));
            m.put("exportPostedMTD", toBD(r,   "posted_mtd_usd"));
        }

        // ── 2. Import invoice KPIs ─────────────────────────────────────────────
        String importSql = """
        SELECT
          COUNT(*) FILTER (WHERE status='DRAFT')     AS draft,
          COUNT(*) FILTER (WHERE status='FINALIZED') AS finalized,
          COUNT(*) FILTER (WHERE status='POSTED')    AS posted,
          COUNT(*) FILTER (WHERE status='CANCELLED') AS cancelled,
          COALESCE(SUM(total_amount)     FILTER (WHERE status='POSTED'), 0) AS posted_usd,
          COALESCE(SUM(total_amount_bdt) FILTER (WHERE status='POSTED'), 0) AS posted_bdt,
          COALESCE(SUM(total_amount)     FILTER (WHERE status='POSTED'
                AND invoice_date >= ?::date), 0) AS posted_mtd_usd,
          COUNT(*)                                    AS total
        FROM com_commercial_invoice
        WHERE invoice_type='IMPORT'""" + f;
        List<Map<String, Object>> impRows = jdbcTemplate.queryForList(importSql, mtdStart);
        if (!impRows.isEmpty()) {
            Map<String, Object> r = impRows.get(0);
            m.put("importDraft",     toLong(r, "draft"));
            m.put("importFinalized", toLong(r, "finalized"));
            m.put("importPosted",    toLong(r, "posted"));
            m.put("importCancelled", toLong(r, "cancelled"));
            m.put("importTotal",     toLong(r, "total"));
            m.put("importPostedUSD", toBD(r,   "posted_usd"));
            m.put("importPostedBDT", toBD(r,   "posted_bdt"));
            m.put("importPostedMTD", toBD(r,   "posted_mtd_usd"));
        }

        // ── 3. LC sub-account KPIs ────────────────────────────────────────────
        String lcSql = """
        SELECT
          COUNT(*) AS total_lcs,
          COUNT(*) FILTER (WHERE lc_status NOT IN ('SETTLED','CANCELLED','EXPIRED')
                               OR lc_status IS NULL) AS active_lcs,
          COALESCE(SUM(lc_amount), 0) AS total_lc_value,
          COALESCE(SUM(lc_amount) FILTER (WHERE expiry_date < ?::date
                AND (lc_status IS NULL OR lc_status NOT IN ('SETTLED','CANCELLED'))), 0)
                                      AS expired_value,
          COUNT(*) FILTER (WHERE expiry_date < ?::date
                AND (lc_status IS NULL OR lc_status NOT IN ('SETTLED','CANCELLED')))
                                      AS expired_lcs,
          COUNT(*) FILTER (WHERE expiry_date BETWEEN ?::date AND (?::date + 30))
                                      AS expiring_30d
        FROM acc_chart_of_accounts_sub
        WHERE sub_account_type = 'LC'""" + (orgId!=null?" AND organization_id="+orgId:"");
        List<Map<String, Object>> lcRows = jdbcTemplate.queryForList(lcSql, today, today, today, today);
        if (!lcRows.isEmpty()) {
            Map<String, Object> r = lcRows.get(0);
            m.put("totalLCs",        toLong(r, "total_lcs"));
            m.put("activeLCs",       toLong(r, "active_lcs"));
            m.put("totalLCValue",    toBD(r,   "total_lc_value"));
            m.put("expiredLCValue",  toBD(r,   "expired_value"));
            m.put("expiredLCCount",  toLong(r, "expired_lcs"));
            m.put("lcsExpiring30d",  toLong(r, "expiring_30d"));
        }

        // ── 4. Settlement KPIs ────────────────────────────────────────────────
        String stlSql = """
        SELECT
          COUNT(*) FILTER (WHERE status='PENDING')  AS pending,
          COUNT(*) FILTER (WHERE status='PARTIAL')  AS partial,
          COUNT(*) FILTER (WHERE status='SETTLED')  AS settled,
          COUNT(*) FILTER (WHERE status='REVERSED') AS reversed,
          COALESCE(SUM(amount_usd) FILTER (WHERE status='SETTLED'), 0) AS settled_usd,
          COALESCE(SUM(amount_bdt) FILTER (WHERE status='SETTLED'), 0) AS settled_bdt,
          COALESCE(SUM(amount_usd) FILTER (WHERE status='SETTLED'
                AND settlement_date >= ?::date), 0)                    AS settled_mtd_usd
        FROM com_lc_settlement WHERE organization_id=""";
        stlSql+=ContextProvider.getOrganizationId();
        List<Map<String, Object>> stlRows = jdbcTemplate.queryForList(stlSql, mtdStart);
        if (!stlRows.isEmpty()) {
            Map<String, Object> r = stlRows.getFirst();
            m.put("pendingSettlements",  toLong(r, "pending"));
            m.put("partialSettlements",  toLong(r, "partial"));
            m.put("settledSettlements",  toLong(r, "settled"));
            m.put("reversedSettlements", toLong(r, "reversed"));
            m.put("settledUSD",          toBD(r,   "settled_usd"));
            m.put("settledBDT",          toBD(r,   "settled_bdt"));
            m.put("settledMTDUSD",       toBD(r,   "settled_mtd_usd"));
        }

        // ── 5. Export by incoterms breakdown ──────────────────────────────────
        m.put("exportByIncoterms", jdbcTemplate.queryForList(
                "SELECT COALESCE(incoterms,'—') AS incoterms," +
                        " COUNT(*) AS count," +
                        " COALESCE(SUM(total_amount),0) AS total_usd" +
                        " FROM com_commercial_invoice WHERE invoice_type='EXPORT' AND status='POSTED'" + f +
                        " GROUP BY incoterms ORDER BY count DESC LIMIT 8"));

        // ── 6. Export by currency ─────────────────────────────────────────────
        m.put("exportByCurrency", jdbcTemplate.queryForList(
                "SELECT COALESCE(currency,'—') AS currency," +
                        " COUNT(*) AS count," +
                        " COALESCE(SUM(total_amount),0) AS total" +
                        " FROM com_commercial_invoice WHERE invoice_type='EXPORT' AND status='POSTED'" + f +
                        " GROUP BY currency ORDER BY total DESC LIMIT 6"));

        // ── 7. Import by currency ─────────────────────────────────────────────
        m.put("importByCurrency", jdbcTemplate.queryForList(
                "SELECT COALESCE(currency,'—') AS currency," +
                        " COUNT(*) AS count," +
                        " COALESCE(SUM(total_amount),0) AS total" +
                        " FROM com_commercial_invoice WHERE invoice_type='IMPORT' AND status='POSTED'" + f +
                        " GROUP BY currency ORDER BY total DESC LIMIT 6"));

        // ── 8. LC settlement type breakdown ───────────────────────────────────
        m.put("settlementByType", jdbcTemplate.queryForList(
                "SELECT COALESCE(settlement_type,'—') AS settlement_type," +
                        " COUNT(*) AS count," +
                        " COALESCE(SUM(amount_usd),0) AS total_usd" +
                        " FROM com_lc_settlement GROUP BY settlement_type ORDER BY count DESC"));

        // ── 9. Recent export invoices ─────────────────────────────────────────
        m.put("recentExports", jdbcTemplate.queryForList("""
        SELECT ci.id, ci.invoice_no, ci.status,
               TO_CHAR(ci.invoice_date,'DD-Mon-YYYY') AS invoice_date,
               COALESCE(ci.currency,'—')              AS currency,
               COALESCE(ci.total_amount,0)            AS total_amount,
               COALESCE(ci.total_amount_bdt,0)        AS total_bdt,
               COALESCE(ci.incoterms,'—')             AS incoterms,
               COALESCE(pt.sub_account_name,'—')      AS party_name,
               COALESCE(ci.vessel_name,'—')           AS vessel_name
        FROM com_commercial_invoice ci
        LEFT JOIN acc_chart_of_accounts_sub pt ON pt.id = ci.party_id
        WHERE ci.invoice_type='EXPORT'""" + fCI + """
         ORDER BY ci.id DESC LIMIT 10 """));

        // ── 10. Recent import invoices ────────────────────────────────────────
        m.put("recentImports", jdbcTemplate.queryForList("""
        SELECT ci.id, ci.invoice_no, ci.status,
               TO_CHAR(ci.invoice_date,'DD-Mon-YYYY') AS invoice_date,
               COALESCE(ci.currency,'—')              AS currency,
               COALESCE(ci.total_amount,0)            AS total_amount,
               COALESCE(ci.total_amount_bdt,0)        AS total_bdt,
               COALESCE(lc.sub_account_name,'—')      AS lc_name,
               COALESCE(pt.sub_account_name,'—')      AS party_name
        FROM com_commercial_invoice ci
        LEFT JOIN acc_chart_of_accounts_sub lc ON lc.id = ci.lc_id
        LEFT JOIN acc_chart_of_accounts_sub pt ON pt.id = ci.party_id
        WHERE ci.invoice_type='IMPORT'""" + fCI + """
        ORDER BY ci.id DESC LIMIT 10"""));

        // ── 11. Active LCs with status ────────────────────────────────────────
        m.put("activeLCList", jdbcTemplate.queryForList("""
        SELECT id, sub_account_code AS lc_no, sub_account_name AS lc_name,
               COALESCE(lc_amount,0) AS lc_amount, COALESCE(currency,'—') AS currency,
               COALESCE(lc_type,'—') AS lc_type, COALESCE(lc_status,'ACTIVE') AS lc_status,
               TO_CHAR(issue_date,'DD-Mon-YYYY')  AS issue_date,
               TO_CHAR(expiry_date,'DD-Mon-YYYY') AS expiry_date,
               CASE WHEN expiry_date < ?::date THEN 'EXPIRED'
                    WHEN expiry_date < (?::date + 30) THEN 'EXPIRING_SOON'
                    ELSE 'ACTIVE' END AS alert
        FROM acc_chart_of_accounts_sub
        WHERE sub_account_type='LC'
          AND (lc_status IS NULL OR lc_status NOT IN ('SETTLED','CANCELLED'))""" +
                (orgId!=null?" AND organization_id="+orgId:"") + """
        ORDER BY expiry_date ASC NULLS LAST
        LIMIT 15""", today, today));

        // ── 12. 12-month export value trend ──────────────────────────────────
        m.put("monthlyExportTrend", jdbcTemplate.queryForList("""
        SELECT TO_CHAR(DATE_TRUNC('month', invoice_date), 'Mon-YY') AS month,
               DATE_TRUNC('month', invoice_date)                    AS month_order,
               COUNT(*) AS invoice_count,
               COALESCE(SUM(total_amount),    0) AS total_usd,
               COALESCE(SUM(total_amount_bdt),0) AS total_bdt
        FROM com_commercial_invoice
        WHERE invoice_type='EXPORT' AND status='POSTED'
          AND invoice_date >= (CURRENT_DATE - INTERVAL '12 months')""" + f + """
        GROUP BY DATE_TRUNC('month', invoice_date)
        ORDER BY month_order"""));

        // ── 13. 12-month import value trend ──────────────────────────────────
        m.put("monthlyImportTrend", jdbcTemplate.queryForList("""
        SELECT TO_CHAR(DATE_TRUNC('month', invoice_date), 'Mon-YY') AS month,
               DATE_TRUNC('month', invoice_date)                    AS month_order,
               COUNT(*) AS invoice_count,
               COALESCE(SUM(total_amount),    0) AS total_usd,
               COALESCE(SUM(total_amount_bdt),0) AS total_bdt
        FROM com_commercial_invoice
        WHERE invoice_type='IMPORT' AND status='POSTED'
          AND invoice_date >= (CURRENT_DATE - INTERVAL '12 months')""" + f + """
        GROUP BY DATE_TRUNC('month', invoice_date)
        ORDER BY month_order"""));

        return m;
    }

// ── Private helpers ──────────────────────────────────────────────────────────
// (Add alongside existing private helpers in CommercialServiceImpl)

    private Long toLong(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private java.math.BigDecimal toBD(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return java.math.BigDecimal.ZERO;
        if (v instanceof java.math.BigDecimal bd) return bd;
        if (v instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        return java.math.BigDecimal.ZERO;
    }


    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private CommercialInvoice buildInvoice(CommercialInvoiceDTO dto, CommercialInvoice e) {
        e.setInvoiceType(CommercialInvoice.InvoiceType.valueOf(dto.getInvoiceType()));
        e.setInvoiceDate(dto.getInvoiceDate());
        e.setCurrency(dto.getCurrency());
        e.setExchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate() : BigDecimal.ONE);
        e.setIncoterms(dto.getIncoterms()); e.setPortOfLoading(dto.getPortOfLoading());
        e.setPortOfDischarge(dto.getPortOfDischarge()); e.setVesselName(dto.getVesselName());
        e.setBlNumber(dto.getBlNumber()); e.setContainerNo(dto.getContainerNo());
        e.setDeliveryId(dto.getDeliveryId()); e.setGrnId(dto.getGrnId());
        e.setRemarks(dto.getRemarks());
        if (dto.getLcId()    != null) e.setLc(subAccRepo.getReferenceById(dto.getLcId()));
        if (dto.getPartyId() != null) e.setParty(subAccRepo.getReferenceById(dto.getPartyId()));
        return e;
    }

    private void syncItems(List<CommercialInvoiceDTO.ItemLineDTO> dtos, CommercialInvoice parent) {
        if (dtos == null) return;
        parent.getItems().clear();
        int num = 1;
        for (CommercialInvoiceDTO.ItemLineDTO ld : dtos) {
            if (ld.getItemId() == null) continue;
            BigDecimal qty  = nvl(ld.getQuantity());
            BigDecimal up   = nvl(ld.getUnitPrice());
            BigDecimal total = qty.multiply(up).setScale(2, RoundingMode.HALF_UP);
            CommercialInvoiceItem item = CommercialInvoiceItem.builder()
                .invoice(parent)
                .item(itemMasterRepo.getReferenceById(ld.getItemId()))
                .quantity(qty).unitPrice(up).totalAmount(total)
                .unit(ld.getUnit()).description(ld.getDescription())
                .deliveryDetailId(ld.getDeliveryDetailId())
                .build();
            parent.getItems().add(item);
            num++;
        }
        invoiceRepo.save(parent);
    }

    private void syncTerms(List<CommercialInvoiceDTO.TermLineDTO> dtos, CommercialInvoice parent) {
        if (dtos == null) return;
        termRepo.deleteByInvoiceId(parent.getId());
        int num = 1;
        for (CommercialInvoiceDTO.TermLineDTO ld : dtos) {
            if (ld.getTitle() == null || ld.getTitle().isBlank()) continue;
            termRepo.save(DocumentTerm.builder()
                .invoice(parent).globalTermsId(ld.getGlobalTermsId())
                .title(ld.getTitle().trim()).description(ld.getDescription())
                .sortOrder(ld.getSortOrder() != null ? ld.getSortOrder() : num)
                .build());
            num++;
        }
    }

    private void recalcTotals(CommercialInvoice inv) {
        List<CommercialInvoiceItem> items = itemRepo.findByInvoiceId(inv.getId());
        BigDecimal total = items.stream().map(CommercialInvoiceItem::getTotalAmount)
            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        inv.setTotalAmount(total);
        BigDecimal rate = inv.getExchangeRate() != null ? inv.getExchangeRate() : BigDecimal.ONE;
        inv.setTotalAmountBdt(total.multiply(rate).setScale(2, RoundingMode.HALF_UP));
        invoiceRepo.save(inv);
    }

    private LcSettlement buildSettlement(LcSettlementDTO dto, LcSettlement e) {
        e.setSettlementDate(dto.getSettlementDate());
        e.setSettlementType(dto.getSettlementType() != null
            ? LcSettlement.SettlementType.valueOf(dto.getSettlementType()) : null);
        e.setExchangeRate(nvl(dto.getExchangeRate())); e.setAmountUsd(nvl(dto.getAmountUsd()));
        e.setAmountBdt(nvl(dto.getAmountBdt())); e.setMarginUsed(nvl(dto.getMarginUsed()));
        e.setCharges(nvl(dto.getCharges())); e.setCommission(nvl(dto.getCommission()));
        e.setInterest(nvl(dto.getInterest())); e.setLoanAmount(nvl(dto.getLoanAmount()));
        if (dto.getLcId() != null) e.setLc(subAccRepo.getReferenceById(dto.getLcId()));
        return e;
    }

    private CommercialInvoice findInvoice(Long id) { return invoiceRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invoice #"+id+" not found.")); }
    private LcSettlement findSettlement(Long id) { return settlementRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Settlement #"+id+" not found.")); }

    private void guardDraft(CommercialInvoice inv) {
        if (inv.getStatus() != CommercialInvoice.InvoiceStatus.DRAFT)
            throw new IllegalStateException("Invoice " + inv.getInvoiceNo() + " is " + inv.getStatus() + " and cannot be modified.");
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
