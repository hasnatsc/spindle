package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.AccountsPolicyDTO;
import com.asg.spindleserp.accounts.service.AccountsPolicyService;
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
 * AccountsPolicyController  /accounts/policy
 *
 * JS fn → endpoint:
 *   policyShow(id)   GET    /accounts/policy/show/{id}
 *   policyEdit(id)   GET    /accounts/policy/show/{id}
 *   policyToggle(id) POST   /accounts/policy/toggle/{id}
 *   policyDelete(id) DELETE /accounts/policy/delete/{id}
 *   (save)           POST   /accounts/policy/save
 */
@Slf4j
@Controller
@RequestMapping("/accounts/policy")
@RequiredArgsConstructor
public class AccountsPolicyController {

    private final AccountsPolicyService policyService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "accounts-policy");
        return "accounts/accounts-policy";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return policyService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", policyService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid AccountsPolicyDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { policyService.update(dto.getId(), dto); res.put("message", "Policy updated."); }
            else                     { policyService.create(dto);              res.put("message", "Policy created."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            AccountsPolicyDTO dto = policyService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Policy " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { policyService.delete(id); res.put("success", true); res.put("message", "Policy deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Voucher type enum values — shared with Mapping form */
    @GetMapping("/voucher-types")
    @ResponseBody
    public List<String> voucherTypes() {
        return Arrays.asList("JOURNAL_VOUCHER","PURCHASE_VOUCHER","SALES_VOUCHER",
                "PAYMENT_VOUCHER","RECEIPT_VOUCHER","CONTRA_VOUCHER",
                "EXPENSE_VOUCHER","DEBIT_NOTE","CREDIT_NOTE");
    }

    /** Module type values */
    @GetMapping("/module-types")
    @ResponseBody
    public List<String> moduleTypes() {
        return Arrays.asList("GENERAL_LEDGER","ACCOUNTS_PAYABLE","ACCOUNTS_RECEIVABLE",
                "CASH_MANAGEMENT","BANK_RECONCILIATION","PROCUREMENT","PURCHASE","SALES",
                "SALES_ORDER","INVENTORY","PRODUCTION","PAYROLL","FIXED_ASSETS","LC_MANAGEMENT","SYSTEM");
    }
}
