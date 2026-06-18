package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.CurrencyDTO;
import com.asg.spindleserp.setup.service.CurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CurrencyController  /currencies
 *
 * JS fn → endpoint:
 *   currShow(id)   GET    /currencies/show/{id}
 *   currEdit(id)   GET    /currencies/show/{id}
 *   currToggle(id) POST   /currencies/toggle/{id}
 *   currDelete(id) DELETE /currencies/delete/{id}
 *   (save)         POST   /currencies/save
 */
@Slf4j
@Controller
@RequestMapping("/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "currencies");
        return "setup/currency-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return currencyService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", currencyService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid CurrencyDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { currencyService.update(dto.getId(), dto); res.put("message", "Currency updated successfully."); }
            else                     { currencyService.create(dto);              res.put("message", "Currency created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            CurrencyDTO dto = currencyService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Currency " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { currencyService.delete(id); res.put("success", true); res.put("message", "Currency deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Dropdown for Country, Bank-Account pickers */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        return currencyService.findActive().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(),
                        "name", c.getName(), "symbol", c.getSymbol() != null ? c.getSymbol() : ""))
                .toList();
    }
}
