package com.asg.spindleserp.budget.service;

import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.budget.dto.*;
import com.asg.spindleserp.budget.entity.*;
import com.asg.spindleserp.budget.repository.*;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.repository.*;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final FiscalYearRepository fyRepo;
    private final BudgetHeadRepository headRepo;
    private final BudgetRepository budgetRepo;
    private final BudgetLineRepository lineRepo;
    private final BudgetRevisionRepository revisionRepo;
    private final BudgetRevisionLineRepository revisionLineRepo;
    private final BudgetTransferRepository transferRepo;
    private final OrganizationRepository orgRepo;
    private final BusinessUnitRepository buRepo;
    private final CostCenterRepository ccRepo;
    private final DepartmentRepository deptRepo;
    private final ChartOfAccountRepository coaRepo;
    private final DocumentSequenceService seqService;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");

    // =========================================================================
    // FISCAL YEAR
    // =========================================================================

    @Override
    public FiscalYearDTO createFiscalYear(FiscalYearDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (fyRepo.findByOrganizationIdAndYearCode(orgId, dto.getYearCode().trim()).isPresent())
            throw new IllegalArgumentException("Fiscal year code '" + dto.getYearCode() + "' already exists.");
        FiscalYear fy = FiscalYear.builder()
                .yearCode(dto.getYearCode().trim())
                .yearName(dto.getYearName().trim())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(FiscalYear.FiscalYearStatus.DRAFT)
                .isCurrent(Boolean.TRUE.equals(dto.getIsCurrent()))
                .notes(dto.getNotes())
                .build();
        fy.setOrganization(orgRepo.getReferenceById(orgId));
        setAudit(fy, true);
        if (Boolean.TRUE.equals(dto.getIsCurrent())) clearCurrentFY(orgId);
        return toDTO(fyRepo.save(fy));
    }

    @Override
    public FiscalYearDTO updateFiscalYear(Long id, FiscalYearDTO dto) {
        FiscalYear fy = findFY(id);
        fy.setYearName(dto.getYearName().trim());
        fy.setStartDate(dto.getStartDate());
        fy.setEndDate(dto.getEndDate());
        fy.setNotes(dto.getNotes());
        if (Boolean.TRUE.equals(dto.getIsCurrent()) && !fy.isCurrent()) {
            clearCurrentFY(fy.getOrganization().getId());
            fy.setCurrent(true);
        }
        setAudit(fy, false);
        return toDTO(fyRepo.save(fy));
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalYearDTO findFiscalYearById(Long id) {
        return toDTO(findFY(id));
    }

    @Override
    public void deleteFiscalYear(Long id) {
        FiscalYear fy = findFY(id);
        if (fy.getStatus() != FiscalYear.FiscalYearStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT fiscal years can be deleted.");
        fyRepo.delete(fy);
    }

    @Override
    public FiscalYearDTO updateFiscalYearStatus(Long id, String newStatus) {
        FiscalYear fy = findFY(id);
        fy.setStatus(FiscalYear.FiscalYearStatus.valueOf(newStatus));
        if ("CLOSED".equals(newStatus)) {
            fy.setClosedBy(ContextProvider.getCurrentUsername());
            fy.setClosedAt(LocalDateTime.now());
            fy.setCurrent(false);
        }
        setAudit(fy, false);
        return toDTO(fyRepo.save(fy));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse fiscalYearDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND fy.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList("fy.year_code", "fy.year_name"));
        String sql = String.format("""
                SELECT ROW_NUMBER() OVER (ORDER BY fy.start_date DESC) AS sl,
                       COUNT(*) OVER ()                                 AS full_count,
                       fy.id, fy.year_code, fy.year_name,
                       TO_CHAR(fy.start_date,'DD-Mon-YYYY') AS start_date,
                       TO_CHAR(fy.end_date,  'DD-Mon-YYYY') AS end_date,
                       fy.status, fy.is_current,
                       CASE WHEN fy.is_current THEN '<span class="badge bg-primary">Current</span>'
                            ELSE '' END AS current_badge,
                       CASE fy.status
                           WHEN 'DRAFT'   THEN '<span class="badge bg-secondary">Draft</span>'
                           WHEN 'ACTIVE'  THEN '<span class="badge bg-success">Active</span>'
                           WHEN 'LOCKED'  THEN '<span class="badge bg-warning text-dark">Locked</span>'
                           WHEN 'CLOSED'  THEN '<span class="badge bg-dark">Closed</span>'
                           ELSE '<span class="badge bg-light text-dark">' || fy.status || '</span>'
                       END AS status_badge,
                       '<div class="btn-group">'
                       || '<a href="javascript:;" onclick="fyShow('   || fy.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                       || CASE WHEN fy.status = 'DRAFT' THEN
                           '<a href="javascript:;" onclick="fyEdit('   || fy.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                           || '<a href="javascript:;" onclick="fyActivate(' || fy.id || ')" class="btn btn-white btn-sm" title="Activate"><i class="fas fa-play-circle text-primary"></i></a>'
                           || '<a href="javascript:;" onclick="fyDelete(' || fy.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                          ELSE '' END
                       || CASE WHEN fy.status = 'ACTIVE' THEN
                           '<a href="javascript:;" onclick="fyLock('  || fy.id || ')" class="btn btn-white btn-sm" title="Lock"><i class="fas fa-lock text-orange"></i></a>'
                          ELSE '' END
                       || CASE WHEN fy.status = 'LOCKED' THEN
                           '<a href="javascript:;" onclick="fyClose(' || fy.id || ')" class="btn btn-white btn-sm" title="Close Year"><i class="fas fa-calendar-times text-danger"></i></a>'
                          ELSE '' END
                       || '</div>' AS actions
                FROM bgt_fiscal_years fy
                %s ORDER BY fy.start_date DESC OFFSET %d LIMIT %d
                """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchFiscalYears(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page - 1) * sz;
        String sql = "SELECT id, year_code, year_name FROM bgt_fiscal_years WHERE status != 'CLOSED'"
                + (orgId != null ? " AND organization_id = " + orgId : "")
                + (q != null && !q.isBlank() ? " AND (year_code ILIKE '%" + q.replace("'", "''") + "%' OR year_name ILIKE '%" + q.replace("'", "''") + "%')" : "")
                + " ORDER BY start_date DESC LIMIT " + (sz + 1) + " OFFSET " + off;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        return Map.of("items", rows.stream().limit(sz).map(r -> Map.of("id", r.get("id"), "text", r.get("year_code") + " — " + r.get("year_name"))).toList(), "hasMore", more).entrySet().stream().map(e -> Map.of(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Override
    public FiscalYearDTO toDTO(FiscalYear e) {
        return FiscalYearDTO.builder()
                .id(e.getId()).yearCode(e.getYearCode()).yearName(e.getYearName())
                .startDate(e.getStartDate()).endDate(e.getEndDate())
                .status(e.getStatus().name()).isCurrent(e.isCurrent())
                .notes(e.getNotes()).closedBy(e.getClosedBy())
                .closedAt(e.getClosedAt() != null ? e.getClosedAt().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
                .build();
    }

    // =========================================================================
    // BUDGET HEAD
    // =========================================================================

    @Override
    public BudgetHeadDTO createHead(BudgetHeadDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (headRepo.existsByOrganizationIdAndHeadCode(orgId, dto.getHeadCode().trim().toUpperCase()))
            throw new IllegalArgumentException("Head code '" + dto.getHeadCode() + "' already exists.");
        BudgetHead head = BudgetHead.builder()
                .headCode(dto.getHeadCode().trim().toUpperCase())
                .headName(dto.getHeadName().trim())
                .headType(BudgetHead.HeadType.valueOf(dto.getHeadType() != null ? dto.getHeadType() : "EXPENSE"))
                .description(dto.getDescription())
                .isActive(Boolean.TRUE.equals(dto.getActive()))
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        head.setOrganization(orgRepo.getReferenceById(orgId));
        if (dto.getParentId() != null) head.setParent(headRepo.getReferenceById(dto.getParentId()));
        setAudit(head, true);
        return toDTO(headRepo.save(head));
    }

    @Override
    public BudgetHeadDTO updateHead(Long id, BudgetHeadDTO dto) {
        BudgetHead head = findHead(id);
        head.setHeadName(dto.getHeadName().trim());
        head.setHeadType(BudgetHead.HeadType.valueOf(dto.getHeadType() != null ? dto.getHeadType() : "EXPENSE"));
        head.setDescription(dto.getDescription());
        head.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        if (dto.getParentId() != null && !dto.getParentId().equals(id))
            head.setParent(headRepo.getReferenceById(dto.getParentId()));
        else head.setParent(null);
        setAudit(head, false);
        return toDTO(headRepo.save(head));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetHeadDTO findHeadById(Long id) {
        return toDTO(findHead(id));
    }

    @Override
    public void deleteHead(Long id) {
        headRepo.delete(findHead(id));
    }

    @Override
    public BudgetHeadDTO toggleHead(Long id) {
        BudgetHead h = findHead(id);
        h.setActive(!h.isActive());
        return toDTO(headRepo.save(h));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse headDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND h.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList("h.head_code", "h.head_name"));
        String sql = String.format("""
                SELECT ROW_NUMBER() OVER (ORDER BY h.display_order, h.head_code) AS sl,
                       COUNT(*) OVER () AS full_count,
                       h.id, h.head_code, h.head_name, h.head_type, h.display_order,
                       COALESCE(p.head_name,'—') AS parent_name,
                       CASE WHEN h.is_active
                           THEN '<span class="badge bg-success">Active</span>'
                           ELSE '<span class="badge bg-secondary">Inactive</span>'
                       END AS status_badge,
                       '<div class="btn-group">'
                       || '<a href="javascript:;" onclick="headEdit('   || h.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                       || '<a href="javascript:;" onclick="headToggle(' || h.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                       || '<a href="javascript:;" onclick="headDelete(' || h.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                       || '</div>' AS actions
                FROM bgt_budget_heads h
                LEFT JOIN bgt_budget_heads p ON p.id = h.parent_id
                %s ORDER BY h.display_order, h.head_code OFFSET %d LIMIT %d
                """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchHeads(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page - 1) * sz;
        String sql = "SELECT id, head_code, head_name FROM bgt_budget_heads WHERE is_active=true"
                + (orgId != null ? " AND organization_id=" + orgId : "")
                + (q != null && !q.isBlank() ? " AND (head_code ILIKE '%" + q.replace("'", "''") + "%' OR head_name ILIKE '%" + q.replace("'", "''") + "%')" : "")
                + " ORDER BY display_order, head_code LIMIT " + (sz + 1) + " OFFSET " + off;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String, Object>> items = rows.stream().limit(sz).map(r ->
                Map.of("id", r.get("id"), "text", r.get("head_code") + " — " + r.get("head_name"),
                        "code", r.get("head_code"), "name", r.get("head_name"))).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public BudgetHeadDTO toDTO(BudgetHead e) {
        BudgetHeadDTO d = BudgetHeadDTO.builder()
                .id(e.getId()).headCode(e.getHeadCode()).headName(e.getHeadName())
                .headType(e.getHeadType() != null ? e.getHeadType().name() : "EXPENSE")
                .description(e.getDescription()).active(e.isActive()).displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
                .build();
        if (e.getParent() != null) {
            d.setParentId(e.getParent().getId());
            d.setParentName(e.getParent().getHeadName());
        }
        return d;
    }

    // =========================================================================
    // BUDGET
    // =========================================================================

    @Override
    public BudgetDTO createBudget(BudgetDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String budgetNo = seqService.nextDocumentNumber(orgId, "BGT", LocalDate.now().format(YY));
        Budget budget = Budget.builder()
                .budgetNo(budgetNo)
                .budgetName(dto.getBudgetName().trim())
                .description(dto.getDescription())
                .fiscalYear(fyRepo.getReferenceById(dto.getFiscalYearId()))
                .businessUnitId(dto.getBusinessUnitId())
                .budgetType(Budget.BudgetType.valueOf(dto.getBudgetType() != null ? dto.getBudgetType() : "ANNUAL"))
                .periodType(dto.getPeriodType() != null ? dto.getPeriodType() : "ANNUAL")
                .periodStart(dto.getPeriodStart())
                .periodEnd(dto.getPeriodEnd())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "BDT")
                .exchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate() : BigDecimal.ONE)
                .overSpendPolicy(Budget.OverSpendPolicy.valueOf(dto.getOverSpendPolicy() != null ? dto.getOverSpendPolicy() : "WARN"))
                .alertThresholdPct(dto.getAlertThresholdPct() != null ? dto.getAlertThresholdPct() : new BigDecimal("80.00"))
                .allowInterLineTransfer(Boolean.TRUE.equals(dto.getAllowInterLineTransfer()))
                .isTemplate(Boolean.TRUE.equals(dto.getIsTemplate()))
                .status(Budget.BudgetStatus.DRAFT)
                .build();
        budget.setOrganization(orgRepo.getReferenceById(orgId));
        setAudit(budget, true);
        Budget saved = budgetRepo.save(budget);
        syncLines(dto.getLines(), saved);
        recalcBudgetTotals(saved);
        return toDTO(budgetRepo.save(saved));
    }

    @Override
    public BudgetDTO updateBudget(Long id, BudgetDTO dto) {
        Budget budget = findBudget(id);
        guardEditable(budget);
        budget.setBudgetName(dto.getBudgetName().trim());
        budget.setDescription(dto.getDescription());
        budget.setPeriodStart(dto.getPeriodStart());
        budget.setPeriodEnd(dto.getPeriodEnd());
        budget.setOverSpendPolicy(Budget.OverSpendPolicy.valueOf(dto.getOverSpendPolicy() != null ? dto.getOverSpendPolicy() : "WARN"));
        budget.setAlertThresholdPct(dto.getAlertThresholdPct() != null ? dto.getAlertThresholdPct() : new BigDecimal("80.00"));
        budget.setAllowInterLineTransfer(Boolean.TRUE.equals(dto.getAllowInterLineTransfer()));
        if (dto.getFiscalYearId() != null) budget.setFiscalYear(fyRepo.getReferenceById(dto.getFiscalYearId()));
        setAudit(budget, false);
        syncLines(dto.getLines(), budget);
        recalcBudgetTotals(budget);
        return toDTO(budgetRepo.save(budget));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetDTO findBudgetById(Long id) {
        return toDTO(findBudget(id));
    }

    @Override
    public void deleteBudget(Long id) {
        Budget b = findBudget(id);
        if (b.getStatus() != Budget.BudgetStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT budgets can be deleted.");
        budgetRepo.delete(b);
    }

    @Override
    public BudgetDTO submitBudget(Long id) {
        return changeStatus(id, Budget.BudgetStatus.SUBMITTED);
    }

    @Override
    public BudgetDTO approveBudget(Long id) {
        return changeStatus(id, Budget.BudgetStatus.APPROVED);
    }

    @Override
    public BudgetDTO activateBudget(Long id) {
        return changeStatus(id, Budget.BudgetStatus.ACTIVE);
    }

    @Override
    public BudgetDTO lockBudget(Long id) {
        return changeStatus(id, Budget.BudgetStatus.LOCKED);
    }

    @Override
    public BudgetDTO closeBudget(Long id) {
        return changeStatus(id, Budget.BudgetStatus.CLOSED);
    }

    @Override
    public BudgetDTO returnBudget(Long id, String remarks) {
        Budget b = findBudget(id);
        b.setStatus(Budget.BudgetStatus.RETURNED);
        b.setDescription((b.getDescription() != null ? b.getDescription() + "\n" : "") + "[RETURNED] " + remarks);
        setAudit(b, false);
        return toDTO(budgetRepo.save(b));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse budgetDatatable(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
                + (orgId != null ? " AND b.organization_id = " + orgId : "")
                + (status != null && !status.isBlank() ? " AND b.status = '" + status + "'" : "")
                + CommonUtils.searchILike(search, Arrays.asList("b.budget_no", "b.budget_name", "fy.year_name"));
        String sql = String.format("""
                SELECT ROW_NUMBER() OVER (ORDER BY b.id DESC) AS sl, COUNT(*) OVER () AS full_count,
                       b.id, b.budget_no, b.budget_name, b.budget_type, b.status,
                       fy.year_code AS fiscal_year,
                       TO_CHAR(b.period_start,'DD-Mon-YYYY') AS period_start,
                       TO_CHAR(b.period_end,  'DD-Mon-YYYY') AS period_end,
                       b.total_budgeted, b.total_actual, b.total_committed, b.total_available,
                       b.currency, b.version,
                       COALESCE(TO_CHAR(b.created_at,'DD-Mon-YYYY'),'—') AS created_at,
                       COALESCE(b.created_by,'—') AS created_by,
                       CASE b.status
                           WHEN 'DRAFT'       THEN '<span class="badge bg-secondary">Draft</span>'
                           WHEN 'SUBMITTED'   THEN '<span class="badge bg-info text-dark">Submitted</span>'
                           WHEN 'IN_APPROVAL' THEN '<span class="badge bg-warning text-dark">In Approval</span>'
                           WHEN 'APPROVED'    THEN '<span class="badge bg-teal">Approved</span>'
                           WHEN 'ACTIVE'      THEN '<span class="badge bg-success">Active</span>'
                           WHEN 'LOCKED'      THEN '<span class="badge bg-orange">Locked</span>'
                           WHEN 'CLOSED'      THEN '<span class="badge bg-dark">Closed</span>'
                           WHEN 'REJECTED'    THEN '<span class="badge bg-danger">Rejected</span>'
                           WHEN 'RETURNED'    THEN '<span class="badge bg-pink">Returned</span>'
                           ELSE '<span class="badge bg-light text-dark">' || b.status || '</span>'
                       END AS status_badge,
                       '<div class="btn-group">'
                       || '<a href="javascript:;" onclick="bgtShow('    || b.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                       || '<a href="javascript:;" onclick="bgtVariance('|| b.id || ')" class="btn btn-white btn-sm" title="Variance"><i class="fas fa-chart-bar text-primary"></i></a>'
                       || CASE WHEN b.status = 'DRAFT' THEN
                           '<a href="javascript:;" onclick="bgtEdit('   || b.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                           || '<a href="javascript:;" onclick="bgtSubmit('|| b.id || ')" class="btn btn-white btn-sm" title="Submit"><i class="fas fa-paper-plane text-blue"></i></a>'
                           || '<a href="javascript:;" onclick="bgtDelete('|| b.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                          ELSE '' END
                       || CASE WHEN b.status = 'SUBMITTED' THEN
                           '<a href="javascript:;" onclick="bgtApprove('|| b.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check-double text-success"></i></a>'
                          ELSE '' END
                       || CASE WHEN b.status = 'APPROVED' THEN
                           '<a href="javascript:;" onclick="bgtActivate('|| b.id || ')" class="btn btn-white btn-sm" title="Activate"><i class="fas fa-play-circle text-teal"></i></a>'
                          ELSE '' END
                       || CASE WHEN b.status = 'ACTIVE' THEN
                           '<a href="javascript:;" onclick="bgtLock('  || b.id || ')" class="btn btn-white btn-sm" title="Lock"><i class="fas fa-lock text-orange"></i></a>'
                          ELSE '' END
                       || '</div>' AS actions
                FROM bgt_budgets b
                JOIN bgt_fiscal_years fy ON fy.id = b.fiscal_year_id
                %s ORDER BY b.id DESC OFFSET %d LIMIT %d
                """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchBudgets(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page - 1) * sz;
        String sql = "SELECT id, budget_no, budget_name FROM bgt_budgets WHERE status NOT IN ('CLOSED','REJECTED')"
                + (orgId != null ? " AND organization_id=" + orgId : "")
                + (q != null && !q.isBlank() ? " AND (budget_no ILIKE '%" + q.replace("'", "''") + "%' OR budget_name ILIKE '%" + q.replace("'", "''") + "%')" : "")
                + " ORDER BY id DESC LIMIT " + (sz + 1) + " OFFSET " + off;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String, Object>> items = rows.stream().limit(sz).map(r -> Map.of("id", r.get("id"), "text", r.get("budget_no") + " — " + r.get("budget_name"))).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public BudgetDTO toDTO(Budget e) {
        List<BudgetLine> lines = lineRepo.findByBudgetIdOrderByLineNumber(e.getId());
        BudgetDTO d = BudgetDTO.builder()
                .id(e.getId()).budgetNo(e.getBudgetNo()).budgetName(e.getBudgetName())
                .description(e.getDescription())
                .fiscalYearId(e.getFiscalYear() != null ? e.getFiscalYear().getId() : null)
                .fiscalYearDisplay(e.getFiscalYear() != null ? e.getFiscalYear().getYearCode() + " — " + e.getFiscalYear().getYearName() : null)
                .businessUnitId(e.getBusinessUnitId())
                .budgetType(e.getBudgetType() != null ? e.getBudgetType().name() : "ANNUAL")
                .periodType(e.getPeriodType()).periodStart(e.getPeriodStart()).periodEnd(e.getPeriodEnd())
                .currency(e.getCurrency()).exchangeRate(e.getExchangeRate())
                .totalBudgeted(e.getTotalBudgeted()).totalRevised(e.getTotalRevised())
                .totalActual(e.getTotalActual()).totalCommitted(e.getTotalCommitted()).totalAvailable(e.getTotalAvailable())
                .status(e.getStatus() != null ? e.getStatus().name() : "DRAFT")
                .overSpendPolicy(e.getOverSpendPolicy() != null ? e.getOverSpendPolicy().name() : "WARN")
                .alertThresholdPct(e.getAlertThresholdPct())
                .allowInterLineTransfer(e.isAllowInterLineTransfer())
                .version(e.getVersion()).isTemplate(e.isTemplate())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
                .lines(lines.stream().map(this::toLineDTO).collect(Collectors.toList()))
                .build();
        if (e.getBusinessUnitId() != null) {
            buRepo.findById(e.getBusinessUnitId()).ifPresent(bu -> d.setBusinessUnitDisplay(bu.getName()));
        }
        return d;
    }

    // =========================================================================
    // BUDGET REVISION
    // =========================================================================

    @Override
    public BudgetRevisionDTO createRevision(BudgetRevisionDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        Budget budget = findBudget(dto.getBudgetId());
        long revNum = revisionRepo.countByBudgetId(budget.getId()) + 1;
        String revNo = seqService.nextDocumentNumber(orgId, "BREV", LocalDate.now().format(YY));
        BudgetRevision revision = BudgetRevision.builder()
                .budget(budget).revisionNo(revNo).revisionNumber((int) revNum)
                .revisionType(BudgetRevision.RevisionType.valueOf(dto.getRevisionType() != null ? dto.getRevisionType() : "REALLOCATION"))
                .reason(dto.getReason()).justification(dto.getJustification())
                .status(BudgetRevision.RevisionStatus.DRAFT)
                .build();
        revision.setOrganization(orgRepo.getReferenceById(orgId));
        setAudit(revision, true);
        BudgetRevision saved = revisionRepo.save(revision);
        // Save revision lines and update budget lines
        if (dto.getLines() != null) {
            BigDecimal totalInc = BigDecimal.ZERO, totalDec = BigDecimal.ZERO;
            for (BudgetRevisionDTO.LineDTO ld : dto.getLines()) {
                BudgetLine bl = findLine(ld.getBudgetLineId());
                BudgetRevisionLine rline = BudgetRevisionLine.builder()
                        .revision(saved).budgetLine(bl)
                        .direction(ld.getDirection())
                        .changeAmount(ld.getChangeAmount())
                        .openingAmount(bl.getRevisedAmount())
                        .closingAmount("+".equals(ld.getDirection())
                                ? bl.getRevisedAmount().add(ld.getChangeAmount())
                                : bl.getRevisedAmount().subtract(ld.getChangeAmount()))
                        .reason(ld.getReason())
                        .build();
                revisionLineRepo.save(rline);
                if ("+".equals(ld.getDirection())) totalInc = totalInc.add(ld.getChangeAmount());
                else totalDec = totalDec.add(ld.getChangeAmount());
            }
            saved.setTotalIncrease(totalInc);
            saved.setTotalDecrease(totalDec);
            revisionRepo.save(saved);
        }
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetRevisionDTO findRevisionById(Long id) {
        return toDTO(revisionRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Revision #" + id + " not found.")));
    }

    @Override
    public BudgetRevisionDTO approveRevision(Long id) {
        BudgetRevision rev = revisionRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Revision #" + id + " not found."));
        // Apply changes to budget lines
        List<BudgetRevisionLine> lines = revisionLineRepo.findByRevisionId(id);
        for (BudgetRevisionLine rl : lines) {
            BudgetLine bl = rl.getBudgetLine();
            if ("+".equals(rl.getDirection())) bl.setRevisedAmount(bl.getRevisedAmount().add(rl.getChangeAmount()));
            else bl.setRevisedAmount(bl.getRevisedAmount().subtract(rl.getChangeAmount()).max(BigDecimal.ZERO));
            lineRepo.save(bl);
        }
        recalcBudgetTotals(rev.getBudget());
        budgetRepo.save(rev.getBudget());
        rev.setStatus(BudgetRevision.RevisionStatus.APPROVED);
        rev.setApprovedBy(ContextProvider.getCurrentUsername());
        rev.setApprovedAt(LocalDateTime.now());
        return toDTO(revisionRepo.save(rev));
    }

    @Override
    public BudgetRevisionDTO rejectRevision(Long id, String remarks) {
        BudgetRevision rev = revisionRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Revision #" + id + " not found."));
        rev.setStatus(BudgetRevision.RevisionStatus.REJECTED);
        rev.setJustification((rev.getJustification() != null ? rev.getJustification() + "\n" : "") + "[REJECTED] " + remarks);
        return toDTO(revisionRepo.save(rev));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse revisionDatatable(int draw, int start, int length, String search, Long budgetId) {
        String where = "WHERE 1=1" + (budgetId != null ? " AND r.budget_id = " + budgetId : "")
                + CommonUtils.searchILike(search, Arrays.asList("r.revision_no", "r.reason"));
        String sql = String.format("""
                SELECT ROW_NUMBER() OVER (ORDER BY r.id DESC) AS sl, COUNT(*) OVER () AS full_count,
                       r.id, r.revision_no, r.revision_number, r.revision_type, r.reason,
                       r.total_increase, r.total_decrease, r.status, r.approved_by,
                       TO_CHAR(r.created_at,'DD-Mon-YYYY') AS created_at,
                       CASE r.status
                           WHEN 'DRAFT'     THEN '<span class="badge bg-secondary">Draft</span>'
                           WHEN 'SUBMITTED' THEN '<span class="badge bg-info text-dark">Submitted</span>'
                           WHEN 'APPROVED'  THEN '<span class="badge bg-success">Approved</span>'
                           WHEN 'REJECTED'  THEN '<span class="badge bg-danger">Rejected</span>'
                           ELSE '<span class="badge bg-light text-dark">' || r.status || '</span>'
                       END AS status_badge,
                       '<div class="btn-group">'
                       || '<a href="javascript:;" onclick="revShow('    || r.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                       || CASE WHEN r.status IN ('DRAFT','SUBMITTED') THEN
                           '<a href="javascript:;" onclick="revApprove('|| r.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check-double text-success"></i></a>'
                           || '<a href="javascript:;" onclick="revReject('|| r.id || ')" class="btn btn-white btn-sm" title="Reject"><i class="fas fa-times-circle text-danger"></i></a>'
                          ELSE '' END
                       || '</div>' AS actions
                FROM bgt_budget_revisions r %s ORDER BY r.id DESC OFFSET %d LIMIT %d
                """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public BudgetRevisionDTO toDTO(BudgetRevision e) {
        BudgetRevisionDTO d = BudgetRevisionDTO.builder()
                .id(e.getId())
                .budgetId(e.getBudget() != null ? e.getBudget().getId() : null)
                .budgetNo(e.getBudget() != null ? e.getBudget().getBudgetNo() : null)
                .budgetName(e.getBudget() != null ? e.getBudget().getBudgetName() : null)
                .revisionNo(e.getRevisionNo()).revisionNumber(e.getRevisionNumber())
                .revisionType(e.getRevisionType() != null ? e.getRevisionType().name() : "REALLOCATION")
                .reason(e.getReason()).justification(e.getJustification())
                .totalIncrease(e.getTotalIncrease()).totalDecrease(e.getTotalDecrease())
                .status(e.getStatus() != null ? e.getStatus().name() : "DRAFT")
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
                .build();
        List<BudgetRevisionLine> rLines = revisionLineRepo.findByRevisionId(e.getId());
        d.setLines(rLines.stream().map(rl -> BudgetRevisionDTO.LineDTO.builder()
                .id(rl.getId()).budgetLineId(rl.getBudgetLine().getId())
                .budgetHeadDisplay(rl.getBudgetLine().getBudgetHead() != null ? rl.getBudgetLine().getBudgetHead().getHeadName() : null)
                .direction(rl.getDirection()).changeAmount(rl.getChangeAmount())
                .openingAmount(rl.getOpeningAmount()).closingAmount(rl.getClosingAmount())
                .reason(rl.getReason())
                .build()).collect(Collectors.toList()));
        return d;
    }

    // =========================================================================
    // BUDGET TRANSFER
    // =========================================================================

    @Override
    public BudgetTransferDTO createTransfer(BudgetTransferDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        Budget budget = findBudget(dto.getBudgetId());
        if (!budget.isAllowInterLineTransfer())
            throw new IllegalStateException("This budget does not allow inter-line transfers.");
        String transferNo = seqService.nextDocumentNumber(orgId, "BTR", LocalDate.now().format(YY));
        BudgetTransfer transfer = BudgetTransfer.builder()
                .budget(budget)
                .fromLine(findLine(dto.getFromLineId()))
                .toLine(findLine(dto.getToLineId()))
                .transferNo(transferNo)
                .transferAmount(dto.getTransferAmount())
                .reason(dto.getReason())
                .transferDate(dto.getTransferDate())
                .status(BudgetTransfer.TransferStatus.PENDING)
                .createdBy(ContextProvider.getCurrentUsername())
                .build();
        String orgStr = String.valueOf(orgId);
        jdbcTemplate.update("UPDATE bgt_transfers SET organization_id = ? WHERE 1=0", orgId); // placeholder
        return toDTO(transferRepo.save(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetTransferDTO findTransferById(Long id) {
        return toDTO(transferRepo.findById(id).orElseThrow());
    }

    @Override
    public BudgetTransferDTO approveTransfer(Long id) {
        BudgetTransfer t = transferRepo.findById(id).orElseThrow();
        // Deduct from source, add to destination
        BudgetLine from = t.getFromLine();
        BudgetLine to = t.getToLine();
        from.setRevisedAmount(from.getRevisedAmount().subtract(t.getTransferAmount()).max(BigDecimal.ZERO));
        to.setRevisedAmount(to.getRevisedAmount().add(t.getTransferAmount()));
        lineRepo.save(from);
        lineRepo.save(to);
        recalcBudgetTotals(t.getBudget());
        budgetRepo.save(t.getBudget());
        t.setStatus(BudgetTransfer.TransferStatus.APPROVED);
        t.setApprovedBy(ContextProvider.getCurrentUsername());
        t.setApprovedAt(LocalDateTime.now());
        return toDTO(transferRepo.save(t));
    }

    @Override
    public BudgetTransferDTO rejectTransfer(Long id) {
        BudgetTransfer t = transferRepo.findById(id).orElseThrow();
        t.setStatus(BudgetTransfer.TransferStatus.REJECTED);
        return toDTO(transferRepo.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse transferDatatable(int draw, int start, int length, String search, Long budgetId) {
        String where = "WHERE 1=1" + (budgetId != null ? " AND t.budget_id = " + budgetId : "")
                + CommonUtils.searchILike(search, Arrays.asList("t.transfer_no", "t.reason"));
        String sql = String.format("""
                SELECT ROW_NUMBER() OVER (ORDER BY t.id DESC) AS sl, COUNT(*) OVER () AS full_count,
                       t.id, t.transfer_no,
                       (fh.head_code || ' ' || fh.head_name) AS from_line,
                       (th.head_code || ' ' || th.head_name) AS to_line,
                       t.transfer_amount, t.reason,
                       TO_CHAR(t.transfer_date,'DD-Mon-YYYY') AS transfer_date,
                       t.status, t.approved_by,
                       CASE t.status
                           WHEN 'PENDING'  THEN '<span class="badge bg-warning text-dark">Pending</span>'
                           WHEN 'APPROVED' THEN '<span class="badge bg-success">Approved</span>'
                           WHEN 'REJECTED' THEN '<span class="badge bg-danger">Rejected</span>'
                       END AS status_badge,
                       '<div class="btn-group">'
                       || CASE WHEN t.status = 'PENDING' THEN
                           '<a href="javascript:;" onclick="tfrApprove('|| t.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check text-success"></i></a>'
                           || '<a href="javascript:;" onclick="tfrReject('|| t.id || ')" class="btn btn-white btn-sm" title="Reject"><i class="fas fa-times text-danger"></i></a>'
                          ELSE '' END
                       || '</div>' AS actions
                FROM bgt_transfers t
                JOIN bgt_budget_lines fl ON fl.id = t.from_line_id
                JOIN bgt_budget_heads fh ON fh.id = fl.budget_head_id
                JOIN bgt_budget_lines tl ON tl.id = t.to_line_id
                JOIN bgt_budget_heads th ON th.id = tl.budget_head_id
                %s ORDER BY t.id DESC OFFSET %d LIMIT %d
                """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public BudgetTransferDTO toDTO(BudgetTransfer e) {
        return BudgetTransferDTO.builder()
                .id(e.getId())
                .budgetId(e.getBudget() != null ? e.getBudget().getId() : null)
                .budgetName(e.getBudget() != null ? e.getBudget().getBudgetName() : null)
                .transferNo(e.getTransferNo())
                .fromLineId(e.getFromLine() != null ? e.getFromLine().getId() : null)
                .fromLineDisplay(e.getFromLine() != null ? lineDisplay(e.getFromLine()) : null)
                .toLineId(e.getToLine() != null ? e.getToLine().getId() : null)
                .toLineDisplay(e.getToLine() != null ? lineDisplay(e.getToLine()) : null)
                .transferAmount(e.getTransferAmount())
                .reason(e.getReason()).transferDate(e.getTransferDate())
                .status(e.getStatus() != null ? e.getStatus().name() : "PENDING")
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }

    // =========================================================================
    // VARIANCE REPORT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> varianceReport(Long budgetId) {
        String sql = """
                SELECT bh.head_code, bh.head_name, bh.head_type,
                       bl.line_number,
                       COALESCE(ac.account_code || ' — ' || ac.account_name, '—') AS account,
                       bl.original_amount, bl.revised_amount, bl.actual_amount, bl.committed_amount,
                       (bl.revised_amount - bl.actual_amount - bl.committed_amount) AS available_amount,
                       CASE WHEN bl.revised_amount > 0
                            THEN ROUND((bl.actual_amount / bl.revised_amount) * 100, 2)
                            ELSE 0
                       END AS utilization_pct,
                       bl.id AS line_id
                FROM bgt_budget_lines bl
                JOIN bgt_budget_heads bh ON bh.id = bl.budget_head_id
                LEFT JOIN acc_chart_of_accounts ac ON ac.id = bl.account_id
                WHERE bl.budget_id = ?
                ORDER BY bh.display_order, bh.head_code, bl.line_number
                """;
        return jdbcTemplate.queryForList(sql, budgetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> budgetSummary(Long budgetId) {
        Budget b = findBudget(budgetId);
        BigDecimal totalBudgeted = b.getTotalBudgeted();
        BigDecimal totalActual = b.getTotalActual();
        BigDecimal totalCommitted = b.getTotalCommitted();
        BigDecimal totalAvailable = b.getTotalAvailable();
        BigDecimal utilizationPct = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalActual.divide(totalBudgeted, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("budgetNo", b.getBudgetNo());
        m.put("budgetName", b.getBudgetName());
        m.put("status", b.getStatus().name());
        m.put("periodStart", b.getPeriodStart());
        m.put("periodEnd", b.getPeriodEnd());
        m.put("totalBudgeted", totalBudgeted);
        m.put("totalActual", totalActual);
        m.put("totalCommitted", totalCommitted);
        m.put("totalAvailable", totalAvailable);
        m.put("utilizationPct", utilizationPct.setScale(2, java.math.RoundingMode.HALF_UP));
        return m;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void syncLines(List<BudgetDTO.LineDTO> dtos, Budget parent) {
        if (dtos == null) return;
        // Clear existing lines
        lineRepo.deleteByBudgetId(parent.getId());
        int num = 1;
        for (BudgetDTO.LineDTO ld : dtos) {
            if (ld.getBudgetHeadId() == null) continue;
            BudgetLine line = BudgetLine.builder()
                    .budget(parent)
                    .budgetHead(headRepo.getReferenceById(ld.getBudgetHeadId()))
                    .lineNumber(num++)
                    .description(ld.getDescription())
                    .originalAmount(nvl(ld.getOriginalAmount()))
                    .revisedAmount(nvl(ld.getRevisedAmount()))
                    .actualAmount(nvl(ld.getActualAmount()))
                    .committedAmount(nvl(ld.getCommittedAmount()))
                    .janAmount(nvl(ld.getJanAmount())).febAmount(nvl(ld.getFebAmount()))
                    .marAmount(nvl(ld.getMarAmount())).aprAmount(nvl(ld.getAprAmount()))
                    .mayAmount(nvl(ld.getMayAmount())).junAmount(nvl(ld.getJunAmount()))
                    .julAmount(nvl(ld.getJulAmount())).augAmount(nvl(ld.getAugAmount()))
                    .sepAmount(nvl(ld.getSepAmount())).octAmount(nvl(ld.getOctAmount()))
                    .novAmount(nvl(ld.getNovAmount())).decAmount(nvl(ld.getDecAmount()))
                    .notes(ld.getNotes())
                    .build();
            if (ld.getAccountId() != null) line.setAccount(coaRepo.getReferenceById(ld.getAccountId()));
            if (ld.getCostCenterId() != null) line.setCostCenter(ccRepo.getReferenceById(ld.getCostCenterId()));
            if (ld.getDepartmentId() != null) line.setDepartment(deptRepo.getReferenceById(ld.getDepartmentId()));
            setAudit(line, true);
            lineRepo.save(line);
        }
    }

    private void recalcBudgetTotals(Budget b) {
        List<BudgetLine> lines = lineRepo.findByBudgetIdOrderByLineNumber(b.getId());
        BigDecimal budgeted = BigDecimal.ZERO, revised = BigDecimal.ZERO, actual = BigDecimal.ZERO, committed = BigDecimal.ZERO;
        for (BudgetLine l : lines) {
            budgeted = budgeted.add(l.getOriginalAmount());
            revised = revised.add(l.getRevisedAmount());
            actual = actual.add(l.getActualAmount());
            committed = committed.add(l.getCommittedAmount());
        }
        b.setTotalBudgeted(budgeted);
        b.setTotalRevised(revised.compareTo(BigDecimal.ZERO) > 0 ? revised : budgeted);
        b.setTotalActual(actual);
        b.setTotalCommitted(committed);
        BigDecimal eff = b.getTotalRevised().compareTo(BigDecimal.ZERO) > 0 ? b.getTotalRevised() : budgeted;
        b.setTotalAvailable(eff.subtract(actual).subtract(committed).max(BigDecimal.ZERO));
    }

    private BudgetDTO.LineDTO toLineDTO(BudgetLine l) {
        BudgetDTO.LineDTO d = BudgetDTO.LineDTO.builder()
                .id(l.getId()).lineNumber(l.getLineNumber()).description(l.getDescription())
                .originalAmount(l.getOriginalAmount()).revisedAmount(l.getRevisedAmount())
                .actualAmount(l.getActualAmount()).committedAmount(l.getCommittedAmount())
                .availableAmount(l.getRevisedAmount().subtract(l.getActualAmount()).subtract(l.getCommittedAmount()))
                .janAmount(l.getJanAmount()).febAmount(l.getFebAmount()).marAmount(l.getMarAmount())
                .aprAmount(l.getAprAmount()).mayAmount(l.getMayAmount()).junAmount(l.getJunAmount())
                .julAmount(l.getJulAmount()).augAmount(l.getAugAmount()).sepAmount(l.getSepAmount())
                .octAmount(l.getOctAmount()).novAmount(l.getNovAmount()).decAmount(l.getDecAmount())
                .notes(l.getNotes())
                .build();
        if (l.getBudgetHead() != null) {
            d.setBudgetHeadId(l.getBudgetHead().getId());
            d.setBudgetHeadDisplay(l.getBudgetHead().getHeadCode() + " — " + l.getBudgetHead().getHeadName());
        }
        if (l.getAccount() != null) {
            d.setAccountId(l.getAccount().getId());
            d.setAccountDisplay(l.getAccount().getAccountCode() + " — " + l.getAccount().getAccountName());
        }
        if (l.getCostCenter() != null) {
            d.setCostCenterId(l.getCostCenter().getId());
            d.setCostCenterDisplay(l.getCostCenter().getCostCenterCode() + " — " + l.getCostCenter().getCostCenterName());
        }
        return d;
    }

    private BudgetDTO changeStatus(Long id, Budget.BudgetStatus newStatus) {
        Budget b = findBudget(id);
        b.setStatus(newStatus);
        setAudit(b, false);
        return toDTO(budgetRepo.save(b));
    }

    private void guardEditable(Budget b) {
        if (b.getStatus() != Budget.BudgetStatus.DRAFT && b.getStatus() != Budget.BudgetStatus.RETURNED)
            throw new IllegalStateException("Budget " + b.getBudgetNo() + " is " + b.getStatus() + " and cannot be edited.");
    }

    private void clearCurrentFY(Long orgId) {
        fyRepo.findByOrganizationIdAndIsCurrentTrue(orgId).ifPresent(fy -> {
            fy.setCurrent(false);
            fyRepo.save(fy);
        });
    }

    private FiscalYear findFY(Long id) {
        return fyRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Fiscal year #" + id + " not found."));
    }

    private BudgetHead findHead(Long id) {
        return headRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Budget head #" + id + " not found."));
    }

    private Budget findBudget(Long id) {
        return budgetRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Budget #" + id + " not found."));
    }

    private BudgetLine findLine(Long id) {
        return lineRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Budget line #" + id + " not found."));
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String lineDisplay(BudgetLine l) {
        return (l.getBudgetHead() != null ? l.getBudgetHead().getHeadCode() + " — " + l.getBudgetHead().getHeadName() : "#" + l.getId());
    }

    private void setAudit(Object entity, boolean isCreate) {
        String user = SecurityHelper.currentUsername().orElse("system");
        if (entity instanceof FiscalYear e) {
            if (isCreate) {
                e.setCreatedBy(user);
                e.setCreatedAt(LocalDateTime.now());
            }
            e.setUpdatedBy(user);
            e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof BudgetHead e) {
            if (isCreate) {
                e.setCreatedBy(user);
                e.setCreatedAt(LocalDateTime.now());
            }
            e.setUpdatedBy(user);
            e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof Budget e) {
            if (isCreate) {
                e.setCreatedBy(user);
                e.setCreatedAt(LocalDateTime.now());
            }
            e.setUpdatedBy(user);
            e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof BudgetLine e) {
            if (isCreate) {
                e.setCreatedBy(user);
                e.setCreatedAt(LocalDateTime.now());
            }
            e.setUpdatedBy(user);
            e.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof BudgetRevision e) {
            if (isCreate) {
                e.setCreatedBy(user);
                e.setCreatedAt(LocalDateTime.now());
            }
            e.setUpdatedBy(user);
            e.setUpdatedAt(LocalDateTime.now());
        }
    }

    // =========================================================================
// DASHBOARD SUMMARY  — add this method to BudgetServiceImpl
// =========================================================================

    /**
     * Org-wide budget dashboard summary.
     * Single-pass conditional-aggregation CTE for all status counts + financials,
     * supplemented by a few small queries for rankings and trends.
     *
     * Uses indexes: idx_bgt_org, idx_bgt_status, idx_bbl_budget, idx_bbl_head,
     *               idx_bbr_budget, idx_bt_budget, idx_ba_date, idx_ba_budget
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String f = orgId != null ? " AND b.organization_id = " + orgId : "";
        String today = LocalDate.now().toString();

        Map<String, Object> result = new LinkedHashMap<>();

        // ── 1. Status counts + aggregate financials ─────────────────────────────
        String kpiSql = """
        SELECT
          COUNT(*) FILTER (WHERE b.status = 'DRAFT')       AS draft,
          COUNT(*) FILTER (WHERE b.status = 'SUBMITTED')   AS submitted,
          COUNT(*) FILTER (WHERE b.status = 'APPROVED')    AS approved,
          COUNT(*) FILTER (WHERE b.status = 'ACTIVE')      AS active,
          COUNT(*) FILTER (WHERE b.status = 'LOCKED')      AS locked,
          COUNT(*) FILTER (WHERE b.status = 'CLOSED')      AS closed,
          COUNT(*) FILTER (WHERE b.status = 'REJECTED')    AS rejected,
          COUNT(*) FILTER (WHERE b.status = 'RETURNED')    AS returned,
          COUNT(*) FILTER (WHERE b.status = 'ACTIVE')      AS total_active,
          COALESCE(SUM(b.total_budgeted)  FILTER (WHERE b.status = 'ACTIVE'), 0) AS total_active_budgeted,
          COALESCE(SUM(b.total_actual)    FILTER (WHERE b.status = 'ACTIVE'), 0) AS total_active_actual,
          COALESCE(SUM(b.total_committed) FILTER (WHERE b.status = 'ACTIVE'), 0) AS total_active_committed,
          COALESCE(SUM(b.total_available) FILTER (WHERE b.status = 'ACTIVE'), 0) AS total_active_available,
          COALESCE(AVG(
              CASE WHEN b.total_budgeted > 0
                   THEN (b.total_actual / b.total_budgeted) * 100
              END) FILTER (WHERE b.status = 'ACTIVE'), 0)              AS avg_utilization_pct
        FROM bgt_budgets b
        WHERE 1=1""" + f;

        List<Map<String, Object>> kpiRows = jdbcTemplate.queryForList(kpiSql);
        if (!kpiRows.isEmpty()) {
            Map<String, Object> r = kpiRows.get(0);
            result.put("draft",     toLong(r, "draft"));
            result.put("submitted", toLong(r, "submitted"));
            result.put("approved",  toLong(r, "approved"));
            result.put("active",    toLong(r, "active"));
            result.put("locked",    toLong(r, "locked"));
            result.put("closed",    toLong(r, "closed"));
            result.put("rejected",  toLong(r, "rejected"));
            result.put("returned",  toLong(r, "returned"));
            result.put("totalActive",           toLong(r, "total_active"));
            result.put("totalActiveBudgeted",   toBD(r, "total_active_budgeted"));
            result.put("totalActiveActual",     toBD(r, "total_active_actual"));
            result.put("totalActiveCommitted",  toBD(r, "total_active_committed"));
            result.put("totalActiveAvailable",  toBD(r, "total_active_available"));
            result.put("avgUtilizationPct",     toBD(r, "avg_utilization_pct"));
        }

        // ── 2. Over-budget lines + alert-threshold lines ────────────────────────
        String alertSql = """
        SELECT
          COUNT(*) FILTER (WHERE bl.actual_amount > bl.revised_amount
                              AND bl.revised_amount > 0)  AS over_budget_line_count,
          COUNT(*) FILTER (WHERE bl.revised_amount > 0
            AND (bl.actual_amount / bl.revised_amount) * 100 >= b.alert_threshold_pct
            AND bl.actual_amount <= bl.revised_amount)    AS alert_line_count
        FROM bgt_budget_lines bl
        JOIN bgt_budgets b ON b.id = bl.budget_id
        WHERE b.status = 'ACTIVE'""" + (orgId != null ? " AND b.organization_id = " + orgId : "");
        List<Map<String, Object>> alertRows = jdbcTemplate.queryForList(alertSql);
        if (!alertRows.isEmpty()) {
            result.put("overBudgetLineCount", toLong(alertRows.get(0), "over_budget_line_count"));
            result.put("alertLineCount",      toLong(alertRows.get(0), "alert_line_count"));
        }

        // ── 3. Pending revisions + transfers ───────────────────────────────────
        String revSql = "SELECT COUNT(*) FROM bgt_budget_revisions r JOIN bgt_budgets b ON b.id = r.budget_id WHERE r.status IN ('DRAFT','SUBMITTED')"
                + (orgId != null ? " AND b.organization_id = " + orgId : "");
        String tfrSql = "SELECT COUNT(*) FROM bgt_transfers t JOIN bgt_budgets b ON b.id = t.budget_id WHERE t.status = 'PENDING'"
                + (orgId != null ? " AND b.organization_id = " + orgId : "");
        result.put("pendingRevisions", jdbcTemplate.queryForObject(revSql, Long.class));
        result.put("pendingTransfers", jdbcTemplate.queryForObject(tfrSql, Long.class));

        // ── 4. Active fiscal year ───────────────────────────────────────────────
        String fySql = "SELECT id, year_code || ' — ' || year_name AS display FROM bgt_fiscal_years WHERE is_current = true"
                + (orgId != null ? " AND organization_id = " + orgId : "") + " LIMIT 1";
        List<Map<String, Object>> fyRows = jdbcTemplate.queryForList(fySql);
        if (!fyRows.isEmpty()) {
            result.put("activeFiscalYear",   fyRows.get(0).get("display"));
            result.put("activeFiscalYearId", fyRows.get(0).get("id"));
        } else {
            result.put("activeFiscalYear",   "—");
            result.put("activeFiscalYearId", null);
        }

        // ── 5. Head-type breakdown (ACTIVE budgets only) ────────────────────────
        String headTypeSql = """
        SELECT bh.head_type,
               COALESCE(SUM(bl.original_amount), 0) AS total_budgeted,
               COALESCE(SUM(bl.actual_amount),   0) AS total_actual,
               COALESCE(SUM(bl.committed_amount),0) AS total_committed
        FROM bgt_budget_lines bl
        JOIN bgt_budget_heads bh ON bh.id = bl.budget_head_id
        JOIN bgt_budgets b ON b.id = bl.budget_id
        WHERE b.status = 'ACTIVE'""" + (orgId != null ? " AND b.organization_id = " + orgId : "") + """
        GROUP BY bh.head_type
        ORDER BY total_budgeted DESC
        """;
        result.put("headTypeBreakdown", jdbcTemplate.queryForList(headTypeSql));

        // ── 6. Top over-spent lines ─────────────────────────────────────────────
        String overspendSql = """
        SELECT b.budget_no, b.budget_name,
               bh.head_name,
               bl.original_amount AS original,
               bl.revised_amount  AS revised,
               bl.actual_amount   AS actual,
               (bl.actual_amount - bl.revised_amount) AS over_by,
               CASE WHEN bl.revised_amount > 0
                    THEN ROUND((bl.actual_amount / bl.revised_amount) * 100, 1)
                    ELSE 0 END AS utilization_pct
        FROM bgt_budget_lines bl
        JOIN bgt_budgets b ON b.id = bl.budget_id
        JOIN bgt_budget_heads bh ON bh.id = bl.budget_head_id
        WHERE b.status = 'ACTIVE'
          AND bl.actual_amount > bl.revised_amount
          AND bl.revised_amount > 0""" + (orgId != null ? " AND b.organization_id = " + orgId : "") + """
        ORDER BY over_by DESC
        LIMIT 10
        """;
        result.put("topOverspentLines", jdbcTemplate.queryForList(overspendSql));

        // ── 7. Top active budgets by utilization ────────────────────────────────
        String topBgtSql = """
        SELECT b.id, b.budget_no, b.budget_name, b.status,
               b.total_budgeted, b.total_revised, b.total_actual,
               b.total_committed, b.total_available,
               CASE WHEN b.total_budgeted > 0
                    THEN ROUND((b.total_actual / b.total_budgeted) * 100, 1)
                    ELSE 0 END AS utilization_pct,
               fy.year_code AS fiscal_year
        FROM bgt_budgets b
        JOIN bgt_fiscal_years fy ON fy.id = b.fiscal_year_id
        WHERE b.status IN ('ACTIVE','LOCKED')""" + (orgId != null ? " AND b.organization_id = " + orgId : "") + """
        ORDER BY utilization_pct DESC
        LIMIT 10
        """;
        result.put("topBudgets", jdbcTemplate.queryForList(topBgtSql));

        // ── 8. 12-month actual spend trend ─────────────────────────────────────
        String trendSql = """
        SELECT TO_CHAR(DATE_TRUNC('month', ba.transaction_date), 'Mon-YY') AS month,
               DATE_TRUNC('month', ba.transaction_date)                    AS month_order,
               COALESCE(SUM(ba.net_amount), 0)                             AS total_actual
        FROM bgt_actuals ba
        JOIN bgt_budgets b ON b.id = ba.budget_id
        WHERE ba.transaction_date >= (CURRENT_DATE - INTERVAL '12 months')""" + (orgId != null ? " AND b.organization_id = " + orgId : "") + """
        GROUP BY DATE_TRUNC('month', ba.transaction_date)
        ORDER BY month_order
        """;
        result.put("monthlyActualTrend", jdbcTemplate.queryForList(trendSql));

        // ── 9. Recent active/submitted budgets list ─────────────────────────────
        String recentSql = """
        SELECT b.id, b.budget_no, b.budget_name, b.budget_type, b.status,
               b.total_budgeted, b.total_actual, b.total_available,
               TO_CHAR(b.period_start,'DD-Mon-YYYY') AS period_start,
               TO_CHAR(b.period_end,  'DD-Mon-YYYY') AS period_end,
               fy.year_code AS fiscal_year,
               CASE WHEN b.total_budgeted > 0
                    THEN ROUND((b.total_actual / b.total_budgeted) * 100, 1)
                    ELSE 0 END AS utilization_pct
        FROM bgt_budgets b
        JOIN bgt_fiscal_years fy ON fy.id = b.fiscal_year_id
        WHERE b.status NOT IN ('REJECTED','CLOSED')""" + (orgId != null ? " AND b.organization_id = " + orgId : "") + """
        ORDER BY b.id DESC
        LIMIT 15
        """;
        result.put("recentBudgets", jdbcTemplate.queryForList(recentSql));

        return result;
    }

// ── Private helpers (add alongside existing helpers in BudgetServiceImpl) ───

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

}
