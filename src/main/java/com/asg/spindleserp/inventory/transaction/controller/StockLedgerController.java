package com.asg.spindleserp.inventory.transaction.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.transaction.dto.StockBalanceDTO;
import com.asg.spindleserp.inventory.transaction.service.StockLedgerService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * StockLedgerController
 *
 * URL: GET /inventory/stocks  → stock balance grid
 * URL: GET /inventory/stocks/ledger → full transaction ledger
 */
@Slf4j
@Controller
@RequestMapping("/inventory/stocks")
@RequiredArgsConstructor
public class StockLedgerController {

    private final StockLedgerService ledgerService;

    // ── Pages ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String balancePage(Model model) {
        model.addAttribute("activePage", "stock-balance");
        return "inventory/stock-balance";
    }

    @GetMapping("/ledger")
    public String ledgerPage(Model model) {
        model.addAttribute("activePage", "stock-ledger");
        return "inventory/stock-ledger";
    }

    // ── Balance DataTable ─────────────────────────────────────────────────────

    @GetMapping("/balance/list")
    @ResponseBody
    public DataTableResponse balanceList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        return ledgerService.balanceDatatable(orgId, warehouseId, itemId, draw, start, length, search);
    }

    // ── Ledger DataTable ──────────────────────────────────────────────────────

    @GetMapping("/ledger/list")
    @ResponseBody
    public DataTableResponse ledgerList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        if (itemId != null)
            return ledgerService.ledgerByItem(itemId, from, to, draw, start, length, search);
        if (warehouseId != null)
            return ledgerService.ledgerByWarehouse(warehouseId, from, to, draw, start, length, search);
        return ledgerService.ledgerAll(orgId, from, to, draw, start, length, search);
    }

    // ── Balance by item (for inline stock check on forms) ────────────────────

    @GetMapping("/balance/by-item/{itemId}")
    @ResponseBody
    public List<StockBalanceDTO> balanceByItem(@PathVariable Long itemId) {
        return ledgerService.balanceByItem(itemId);
    }

    @GetMapping("/balance/by-warehouse/{warehouseId}")
    @ResponseBody
    public List<StockBalanceDTO> balanceByWarehouse(@PathVariable Long warehouseId) {
        return ledgerService.balanceByWarehouse(warehouseId);
    }

    /** Available qty for a specific item × warehouse × lot — used by form validation */
    @GetMapping("/balance/available")
    @ResponseBody
    public java.math.BigDecimal availableQty(
            @RequestParam Long itemId,
            @RequestParam Long warehouseId,
            @RequestParam(required = false) Long lotId) {
        return ledgerService.availableQty(itemId, warehouseId, lotId);
    }
}
