package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.HsCodeDTO;
import com.asg.spindleserp.setup.entity.HsCode;
import com.asg.spindleserp.setup.service.HsCodeService;
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
 * HsCodeController  /hs-codes
 *
 * JS fn → endpoint:
 *   hsShow(id)   GET    /hs-codes/show/{id}
 *   hsEdit(id)   GET    /hs-codes/show/{id}
 *   hsToggle(id) POST   /hs-codes/toggle/{id}
 *   hsDelete(id) DELETE /hs-codes/delete/{id}
 *   (save)       POST   /hs-codes/save
 */
@Slf4j
@Controller
@RequestMapping("/hs-codes")
@RequiredArgsConstructor
public class HsCodeController {

    private final HsCodeService hsCodeService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "hs-codes");
        return "setup/hs-code-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return hsCodeService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", hsCodeService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid HsCodeDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { hsCodeService.update(dto.getId(), dto); res.put("message", "HS Code updated successfully."); }
            else                     { hsCodeService.create(dto);              res.put("message", "HS Code created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            HsCodeDTO dto = hsCodeService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "HS Code " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { hsCodeService.delete(id); res.put("success", true); res.put("message", "HS Code deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Active HS codes for the current org — for commercial / LC item pickers */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<HsCodeDTO> list = orgId != null ? hsCodeService.findActiveByOrg(orgId)
                : hsCodeService.findAll().stream().filter(h -> Boolean.TRUE.equals(h.getActive())).toList();
        return list.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          h.getId());
            m.put("code",        h.getHsCode());
            m.put("description", h.getShortDescription() != null ? h.getShortDescription() : h.getDescription());
            m.put("hsType",      h.getHsType());
            return m;
        }).toList();
    }

    /** HsType enum values */
    @GetMapping("/hs-types")
    @ResponseBody
    public List<String> hsTypes() {
        return Arrays.stream(HsCode.HsType.values()).map(Enum::name).toList();
    }
}
