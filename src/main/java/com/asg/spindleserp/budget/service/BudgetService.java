package com.asg.spindleserp.budget.service;

import com.asg.spindleserp.budget.dto.*;
import com.asg.spindleserp.budget.entity.*;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

/**
 * BudgetService — unified interface for all budget-module operations.
 *
 * Covers:
 *   Fiscal Year management
 *   Budget Head (category) setup
 *   Budget CRUD with line items + monthly phasing
 *   Budget lifecycle: submit → approve → activate → lock → close
 *   Budget Revisions (supplementary / reallocation)
 *   Inter-line Budget Transfers
 *   Variance report (budgeted vs actual vs committed)
 *   Dashboard summary (org-wide KPIs)
 */
public interface BudgetService {

    // ── Fiscal Year ───────────────────────────────────────────────────────────

    FiscalYearDTO createFiscalYear(FiscalYearDTO dto);
    FiscalYearDTO updateFiscalYear(Long id, FiscalYearDTO dto);
    FiscalYearDTO findFiscalYearById(Long id);
    void          deleteFiscalYear(Long id);
    /** Activate / lock / close the fiscal year */
    FiscalYearDTO updateFiscalYearStatus(Long id, String newStatus);
    DataTableResponse fiscalYearDatatable(int draw, int start, int length, String search);
    List<Map<String, Object>> searchFiscalYears(String q, int page);
    FiscalYearDTO toDTO(FiscalYear entity);

    // ── Budget Head ───────────────────────────────────────────────────────────

    BudgetHeadDTO createHead(BudgetHeadDTO dto);
    BudgetHeadDTO updateHead(Long id, BudgetHeadDTO dto);
    BudgetHeadDTO findHeadById(Long id);
    void          deleteHead(Long id);
    BudgetHeadDTO toggleHead(Long id);
    DataTableResponse headDatatable(int draw, int start, int length, String search);
    Map<String, Object> searchHeads(String q, int page);
    BudgetHeadDTO toDTO(BudgetHead entity);

    // ── Budget ────────────────────────────────────────────────────────────────

    BudgetDTO createBudget(BudgetDTO dto);
    BudgetDTO updateBudget(Long id, BudgetDTO dto);
    BudgetDTO findBudgetById(Long id);
    void      deleteBudget(Long id);

    /** Status transitions:
     *   DRAFT → SUBMITTED → APPROVED → ACTIVE → LOCKED → CLOSED
     *   SUBMITTED → RETURNED / REJECTED
     */
    BudgetDTO submitBudget(Long id);
    BudgetDTO approveBudget(Long id);
    BudgetDTO activateBudget(Long id);
    BudgetDTO lockBudget(Long id);
    BudgetDTO closeBudget(Long id);
    BudgetDTO returnBudget(Long id, String remarks);

    DataTableResponse budgetDatatable(int draw, int start, int length, String search, String status);
    Map<String, Object> searchBudgets(String q, int page);
    BudgetDTO toDTO(Budget entity);

    // ── Budget Revision ───────────────────────────────────────────────────────

    BudgetRevisionDTO createRevision(BudgetRevisionDTO dto);
    BudgetRevisionDTO findRevisionById(Long id);
    BudgetRevisionDTO approveRevision(Long id);
    BudgetRevisionDTO rejectRevision(Long id, String remarks);
    DataTableResponse revisionDatatable(int draw, int start, int length, String search, Long budgetId);
    BudgetRevisionDTO toDTO(BudgetRevision entity);

    // ── Budget Transfer ───────────────────────────────────────────────────────

    BudgetTransferDTO createTransfer(BudgetTransferDTO dto);
    BudgetTransferDTO findTransferById(Long id);
    BudgetTransferDTO approveTransfer(Long id);
    BudgetTransferDTO rejectTransfer(Long id);
    DataTableResponse transferDatatable(int draw, int start, int length, String search, Long budgetId);
    BudgetTransferDTO toDTO(BudgetTransfer entity);

    // ── Variance Report ───────────────────────────────────────────────────────

    /** Returns budgeted vs actual vs committed per head per line for a budget */
    List<Map<String, Object>> varianceReport(Long budgetId);

    /** Summary KPIs for the dashboard panel (single budget) */
    Map<String, Object> budgetSummary(Long budgetId);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    /**
     * Org-wide budget dashboard summary.
     * Called by GET /budget/dashboard/summary.
     *
     * Response shape:
     * {
     *   draft, submitted, approved, active, locked, closed, rejected, returned,
     *   totalActive, totalActiveBudgeted, totalActiveActual,
     *   totalActiveCommitted, totalActiveAvailable, avgUtilizationPct,
     *   overBudgetLineCount, alertLineCount, pendingRevisions, pendingTransfers,
     *   activeFiscalYear, activeFiscalYearId,
     *   headTypeBreakdown: [ {headType, totalBudgeted, totalActual} ],
     *   topOverspentLines: [ {budgetNo, headName, original, actual, overBy} ],
     *   topBudgets:        [ {budgetNo, budgetName, status, totalBudgeted, utilizationPct} ],
     *   monthlyActualTrend:[ {month, totalActual} ]  — 12 months from bgt_actuals
     * }
     */
    Map<String, Object> dashboardSummary();
}
