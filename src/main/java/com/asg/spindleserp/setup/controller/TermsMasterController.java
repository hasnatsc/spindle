package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.TermsMasterDTO;
import com.asg.spindleserp.setup.service.TermsMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TermsMasterController  /terms
 *
 * JS fn → endpoint:
 *   termsShow(id)   GET    /terms/show/{id}
 *   termsEdit(id)   GET    /terms/show/{id}
 *   termsToggle(id) POST   /terms/toggle/{id}
 *   termsDelete(id) DELETE /terms/delete/{id}
 *   (save)          POST   /terms/save
 */
@Slf4j
@Controller
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsMasterController {

    private final TermsMasterService termsService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "terms");
        return "setup/terms-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return termsService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", termsService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid TermsMasterDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { termsService.update(dto.getId(), dto); res.put("message", "Terms updated successfully."); }
            else                     { termsService.create(dto);              res.put("message", "Terms created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            TermsMasterDTO dto = termsService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Terms " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { termsService.delete(id); res.put("success", true); res.put("message", "Terms deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /**
     * Active terms by document type — for form dropdowns.
     * GET /terms/active?documentType=PURCHASE_ORDER
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active(@RequestParam(defaultValue = "") String documentType) {
        List<TermsMasterDTO> list = documentType.isBlank()
                ? termsService.findAll().stream().filter(t -> Boolean.TRUE.equals(t.getActive())).toList()
                : termsService.findActiveByDocumentType(documentType);
        return list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",           t.getId());
            m.put("title",        t.getTitle());
            m.put("documentType", t.getDocumentType());
            m.put("isDefault",    t.getIsDefault());
            return m;
        }).toList();
    }
}
