package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.AccountingPeriodDTO;
import com.asg.spindleserp.accounts.entity.AccountingPeriod;
import com.asg.spindleserp.accounts.service.AccountingPeriodService;
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
 * AccountingPeriodController  /accounts/periods
 *
 * JS fn → endpoint:
 *   periodShow(id)   GET    /accounts/periods/show/{id}
 *   periodEdit(id)   GET    /accounts/periods/show/{id}
 *   periodToggle(id) POST   /accounts/periods/toggle/{id}
 *   periodClose(id)  POST   /accounts/periods/close/{id}
 *   periodDelete(id) DELETE /accounts/periods/delete/{id}
 *   (save)           POST   /accounts/periods/save
 */
@Slf4j
@Controller
@RequestMapping("/accounts/periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService periodService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "accounting-periods");
        return "accounts/accounting-periods";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return periodService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", periodService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid AccountingPeriodDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { periodService.update(dto.getId(), dto); res.put("message", "Period updated."); }
            else                     { periodService.create(dto);              res.put("message", "Period created."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            AccountingPeriodDTO dto = periodService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Period " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/close/{id}")
    @ResponseBody
    public Map<String, Object> close(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { periodService.closePeriod(id); res.put("success", true); res.put("message", "Period closed successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { periodService.delete(id); res.put("success", true); res.put("message", "Period deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/types")
    @ResponseBody
    public List<String> types() {
        return Arrays.stream(AccountingPeriod.PeriodType.values()).map(Enum::name).toList();
    }
}
