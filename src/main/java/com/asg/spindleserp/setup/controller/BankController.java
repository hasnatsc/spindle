package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.BankDTO;
import com.asg.spindleserp.setup.entity.Bank;
import com.asg.spindleserp.setup.service.BankService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BankController  /banks
 *
 * JS fn → endpoint:
 *   bankShow(id)   GET    /banks/show/{id}
 *   bankEdit(id)   GET    /banks/show/{id}
 *   bankToggle(id) POST   /banks/toggle/{id}
 *   bankDelete(id) DELETE /banks/delete/{id}
 *   (save)         POST   /banks/save
 */
@Slf4j
@Controller
@RequestMapping("/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "banks");
        return "setup/bank-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return bankService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", bankService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid BankDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { bankService.update(dto.getId(), dto); res.put("message", "Bank updated successfully."); }
            else                     { bankService.create(dto);              res.put("message", "Bank created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BankDTO dto = bankService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Bank " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { bankService.delete(id); res.put("success", true); res.put("message", "Bank deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Active banks for the current org — for bank-account / LC pickers */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<BankDTO> list = orgId != null ? bankService.findActiveByOrg(orgId)
                : bankService.findAll().stream().filter(b -> Boolean.TRUE.equals(b.getActive())).toList();
        return list.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        b.getId());
            m.put("code",      b.getBankCode());
            m.put("name",      b.getBankName());
            m.put("swiftCode", b.getSwiftCode() != null ? b.getSwiftCode() : "");
            m.put("supportsLc", b.getSupportsLc());
            return m;
        }).toList();
    }

    /** LC-capable banks only — for LC form pickers */
    @GetMapping("/lc-banks")
    @ResponseBody
    public List<Map<String, Object>> lcBanks() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        if (orgId == null) return List.of();
        return bankService.findLcBanksByOrg(orgId).stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        b.getId());
            m.put("code",      b.getBankCode());
            m.put("name",      b.getBankName());
            m.put("swiftCode", b.getSwiftCode() != null ? b.getSwiftCode() : "");
            return m;
        }).toList();
    }

    /** BankType enum values */
    @GetMapping("/bank-types")
    @ResponseBody
    public List<String> bankTypes() {
        return Arrays.stream(Bank.BankType.values()).map(Enum::name).toList();
    }

    /** BankRating enum values */
    @GetMapping("/bank-ratings")
    @ResponseBody
    public List<String> bankRatings() {
        return Arrays.stream(Bank.BankRating.values()).map(Enum::name).toList();
    }
}
