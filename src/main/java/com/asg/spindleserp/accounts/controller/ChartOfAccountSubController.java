package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.service.ChartOfAccountSubService;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChartOfAccountSubController
 *
 * Serves pages for BANK | CASH | CUSTOMER | SUPPLIER | EMPLOYEE | LC | GENERAL sub-accounts.
 * Each type has its own page URL but shares one modal save/show endpoint.
 *
 * URL patterns (page):
 *   GET /accounts/bank-accounts          → bank-accounts.html
 *   GET /accounts/cash-accounts          → cash-accounts.html
 *   GET /accounts/customer-accounts      → customer-accounts.html
 *   GET /accounts/supplier-accounts      → supplier-accounts.html
 *   GET /accounts/employee-accounts      → employee-accounts.html
 *   GET /accounts/sub-accounts           → sub-accounts.html  (all types)
 *
 * Shared REST endpoints (all types):
 *   GET    /accounts/sub-accounts/list?subAccountType=BANK
 *   GET    /accounts/sub-accounts/show/{id}
 *   POST   /accounts/sub-accounts/save
 *   POST   /accounts/sub-accounts/toggle/{id}
 *   DELETE /accounts/sub-accounts/delete/{id}
 *   GET    /accounts/sub-accounts/search?search=&subAccountType=&page=
 *
 * JS fn prefix per type:
 *   BANK → bankShow/bankEdit/bankToggle/bankDelete
 *   CASH → cashShow/cashEdit/cashToggle/cashDelete
 *   CUSTOMER → custShow/custEdit/custToggle/custDelete
 *   SUPPLIER → suppShow/suppEdit/suppToggle/suppDelete
 *   EMPLOYEE → empShow/empEdit/empToggle/empDelete
 *   LC       → lcShow/lcEdit/lcToggle/lcDelete
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChartOfAccountSubController {

    private final ChartOfAccountSubService subService;

    // ── Pages ──────────────────────────────────────────────────────────────────

    @GetMapping("/accounts/bank-accounts")
    public String bankPage(Model model) {
        model.addAttribute("activePage", "bank-accounts");
        model.addAttribute("subAccountType", "BANK");
        return "accounts/bank-accounts";
    }

    @GetMapping("/accounts/cash-accounts")
    public String cashPage(Model model) {
        model.addAttribute("activePage", "cash-accounts");
        model.addAttribute("subAccountType", "CASH");
        return "accounts/cash-accounts";
    }

    @GetMapping("/accounts/customer-accounts")
    public String customerPage(Model model) {
        model.addAttribute("activePage", "customer-accounts");
        model.addAttribute("subAccountType", "CUSTOMER");
        return "accounts/customer-accounts";
    }

    @GetMapping("/accounts/supplier-accounts")
    public String supplierPage(Model model) {
        model.addAttribute("activePage", "supplier-accounts");
        model.addAttribute("subAccountType", "SUPPLIER");
        return "accounts/supplier-accounts";
    }

    @GetMapping("/accounts/employee-accounts")
    public String employeePage(Model model) {
        model.addAttribute("activePage", "employee-accounts");
        model.addAttribute("subAccountType", "EMPLOYEE");
        return "accounts/employee-accounts";
    }

    @GetMapping("/accounts/sub-accounts")
    public String allSubPage(Model model) {
        model.addAttribute("activePage", "sub-accounts");
        model.addAttribute("subAccountType", "");
        return "accounts/sub-accounts";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/accounts/sub-accounts/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "")   String subAccountType) {
        return subService.datatableList(subAccountType, draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/accounts/sub-accounts/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", subService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @PostMapping("/accounts/sub-accounts/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ChartOfAccountSubDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                subService.update(dto.getId(), dto);
                res.put("message", "Account updated successfully.");
            } else {
                subService.create(dto);
                res.put("message", "Account created successfully.");
            }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    @PostMapping("/accounts/sub-accounts/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ChartOfAccountSubDTO dto = subService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Account " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/accounts/sub-accounts/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { subService.delete(id); res.put("success", true); res.put("message", "Account deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AJAX Select2 search ───────────────────────────────────────────────────

    /**
     * GET /accounts/sub-accounts/search?search=abc&subAccountType=CUSTOMER&page=1
     * Returns: { items: [{id, text, code, name, subAccountType}], hasMore }
     */
    @GetMapping("/accounts/sub-accounts/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "")   String subAccountType,
            @RequestParam(defaultValue = "1")  int    page,
            @RequestParam(defaultValue = "30") int    pageSize) {
        return subService.search(search, subAccountType, page, pageSize);
    }

    // ── Reference data ────────────────────────────────────────────────────────

    @GetMapping("/accounts/sub-accounts/types")
    @ResponseBody
    public List<String> subAccountTypes() {
        return Arrays.stream(ChartOfAccountSub.SubAccountType.values()).map(Enum::name).toList();
    }
}
