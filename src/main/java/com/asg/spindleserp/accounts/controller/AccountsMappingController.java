package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.dto.AccountsMappingDTO;
import com.asg.spindleserp.accounts.service.AccountsMappingService;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AccountsMappingController  /accounts/mapping
 *
 * JS fn → endpoint:
 *   mapShow(id)   GET    /accounts/mapping/show/{id}
 *   mapEdit(id)   GET    /accounts/mapping/show/{id}
 *   mapToggle(id) POST   /accounts/mapping/toggle/{id}
 *   mapDelete(id) DELETE /accounts/mapping/delete/{id}
 *   (save)        POST   /accounts/mapping/save
 *   (search)      GET    /accounts/mapping/search
 */
@Slf4j
@Controller
@RequestMapping("/accounts/mapping")
@RequiredArgsConstructor
public class AccountsMappingController {

    private final AccountsMappingService mappingService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "accounts-mapping");
        return "accounts/accounts-mapping";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return mappingService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", mappingService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid AccountsMappingDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { mappingService.update(dto.getId(), dto); res.put("message", "Mapping updated."); }
            else                     { mappingService.create(dto);              res.put("message", "Mapping created."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            AccountsMappingDTO dto = mappingService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Mapping " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { mappingService.delete(id); res.put("success", true); res.put("message", "Mapping deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "1")  int    page,
            @RequestParam(defaultValue = "30") int    pageSize) {
        return mappingService.search(search, page, pageSize);
    }
}
