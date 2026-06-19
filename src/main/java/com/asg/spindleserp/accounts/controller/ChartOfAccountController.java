package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.ChartOfAccountDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.service.ChartOfAccountService;
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
import java.util.stream.Collectors;

/**
 * ChartOfAccountController  /accounts/chart
 *
 * JS fn → endpoint:
 *   coaShow(id)   GET    /accounts/chart/show/{id}
 *   coaEdit(id)   GET    /accounts/chart/show/{id}
 *   coaToggle(id) POST   /accounts/chart/toggle/{id}
 *   coaDelete(id) DELETE /accounts/chart/delete/{id}
 *   (save)        POST   /accounts/chart/save
 *   (search)      GET    /accounts/chart/search?search=&page=
 */
@Slf4j
@Controller
@RequestMapping("/accounts/chart")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService coaService;

    // ── Page ──────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "chart-of-accounts");
        return "accounts/chart-of-accounts";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return coaService.datatableList(draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", coaService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ChartOfAccountDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                coaService.update(dto.getId(), dto);
                res.put("message", "Account updated successfully.");
            } else {
                coaService.create(dto);
                res.put("message", "Account created successfully.");
            }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ChartOfAccountDTO dto = coaService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Account " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { coaService.delete(id); res.put("success", true); res.put("message", "Account deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── AJAX Select2 search ───────────────────────────────────────────────────

    /**
     * GET /accounts/chart/search?search=cash&page=1&pageSize=30
     * Returns: { items: [{id, text, code, name, accountType, nature}], hasMore }
     */
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "1")  int    page,
            @RequestParam(defaultValue = "30") int    pageSize) {
        return coaService.search(search, page, pageSize);
    }

    // ── Reference data ────────────────────────────────────────────────────────

    /** Account type enum values for form dropdowns */
    @GetMapping("/account-types")
    @ResponseBody
    public List<String> accountTypes() {
        return Arrays.stream(ChartOfAccount.AccountType.values()).map(Enum::name).toList();
    }

    /** Account nature enum values */
    @GetMapping("/account-natures")
    @ResponseBody
    public List<String> accountNatures() {
        return Arrays.stream(ChartOfAccount.AccountNature.values()).map(Enum::name).toList();
    }
}
