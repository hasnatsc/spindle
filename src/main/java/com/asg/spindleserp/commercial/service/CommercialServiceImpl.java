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
                   || CASE WHEN ci.status = ''DRAFT'' THEN
                       '<a href="javascript:;" onclick="ciEdit('     || ci.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                       || '<a href="javascript:;" onclick="ciFinalize('|| ci.id || ')" class="btn btn-white btn-sm" title="Finalize"><i class="fas fa-check text-teal"></i></a>'
                       || '<a href="javascript:;" onclick="ciDelete(' || ci.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                      END
                   || CASE WHEN ci.status = ''FINALIZED'' THEN
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
                   || CASE WHEN s.status IN (''PENDING'',''PARTIAL'') THEN
                       '<a href="javascript:;" onclick="stlEdit('   || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                       || '<a href="javascript:;" onclick="stlSettle('|| s.id || ')" class="btn btn-white btn-sm" title="Mark Settled"><i class="fas fa-check-double text-success"></i></a>'
                       || '<a href="javascript:;" onclick="stlDelete('|| s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                      END
                   || CASE WHEN s.status = ''SETTLED'' THEN
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

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String f = orgId != null ? " AND organization_id=" + orgId : "";
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("exportDraft",     jdbcTemplate.queryForObject("SELECT COUNT(*) FROM com_commercial_invoice WHERE invoice_type='EXPORT' AND status='DRAFT'" + f, Long.class));
        m.put("exportFinalized", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM com_commercial_invoice WHERE invoice_type='EXPORT' AND status='FINALIZED'" + f, Long.class));
        m.put("importDraft",     jdbcTemplate.queryForObject("SELECT COUNT(*) FROM com_commercial_invoice WHERE invoice_type='IMPORT' AND status='DRAFT'" + f, Long.class));
        m.put("importFinalized", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM com_commercial_invoice WHERE invoice_type='IMPORT' AND status='FINALIZED'" + f, Long.class));
        m.put("pendingSettlements", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM com_lc_settlement WHERE status='PENDING'", Long.class));
        m.put("totalSettlements",   jdbcTemplate.queryForObject("SELECT COUNT(*) FROM com_lc_settlement WHERE status='SETTLED'", Long.class));
        return m;
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
