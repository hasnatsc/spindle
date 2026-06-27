package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.service.GlReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GlReportController — General Ledger, Trial Balance, P&L, Balance Sheet.
 *
 * Pages (Thymeleaf):
 *   GET /accounts/ledger          → accounts/ledger.html
 *   GET /accounts/trial-balance   → accounts/trial-balance.html
 *   GET /accounts/profit-loss     → accounts/profit-loss.html
 *   GET /accounts/balance-sheet   → accounts/balance-sheet.html
 *
 * Data endpoints (JSON):
 *   GET /accounts/ledger/data?accountId=&startDate=&endDate=
 *   GET /accounts/trial-balance/data?asOfDate=&showZeroBalance=
 *   GET /accounts/profit-loss/data?startDate=&endDate=&compareStartDate=&compareEndDate=
 *   GET /accounts/balance-sheet/data?asOfDate=&compareDate=
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GlReportController {

    private final GlReportService glReportService;

    // ── Pages ─────────────────────────────────────────────────────────────────

    @GetMapping("/accounts/ledger")
    public String ledgerPage(Model model) {
        model.addAttribute("activePage", "gl-ledger");
        return "accounts/ledger";
    }

    @GetMapping("/accounts/trial-balance")
    public String trialBalancePage(Model model) {
        model.addAttribute("activePage", "trial-balance");
        return "accounts/trial-balance";
    }

    @GetMapping("/accounts/profit-loss")
    public String profitLossPage(Model model) {
        model.addAttribute("activePage", "profit-loss");
        return "accounts/profit-loss";
    }

    @GetMapping("/accounts/balance-sheet")
    public String balanceSheetPage(Model model) {
        model.addAttribute("activePage", "balance-sheet");
        return "accounts/balance-sheet";
    }

    // ── Data endpoints ────────────────────────────────────────────────────────

    @GetMapping("/accounts/ledger/data")
    @ResponseBody
    public Map<String, Object> ledgerData(
            @RequestParam Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            return glReportService.generalLedger(accountId, startDate, endDate);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/accounts/trial-balance/data")
    @ResponseBody
    public Map<String, Object> trialBalanceData(
            @RequestParam(required = false) String asOfDate,
            @RequestParam(defaultValue = "false") boolean showZeroBalance) {
        try {
            return glReportService.trialBalance(asOfDate, showZeroBalance);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/accounts/profit-loss/data")
    @ResponseBody
    public Map<String, Object> profitLossData(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String compareStartDate,
            @RequestParam(required = false) String compareEndDate) {
        try {
            return glReportService.profitAndLoss(startDate, endDate, compareStartDate, compareEndDate);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/accounts/balance-sheet/data")
    @ResponseBody
    public Map<String, Object> balanceSheetData(
            @RequestParam(required = false) String asOfDate,
            @RequestParam(required = false) String compareDate) {
        try {
            return glReportService.balanceSheet(asOfDate, compareDate);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
