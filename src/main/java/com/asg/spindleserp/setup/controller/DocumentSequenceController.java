package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.DocumentSequenceDTO;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import com.asg.spindleserp.security.auth.SecurityHelper;
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
 * DocumentSequenceController  /document-sequences
 *
 * JS fn → endpoint:
 *   seqShow(id)   GET    /document-sequences/show/{id}
 *   seqEdit(id)   GET    /document-sequences/show/{id}
 *   seqDelete(id) DELETE /document-sequences/delete/{id}
 *   (save)        POST   /document-sequences/save
 *
 * No toggle — sequences have no active/inactive flag.
 */
@Slf4j
@Controller
@RequestMapping("/document-sequences")
@RequiredArgsConstructor
public class DocumentSequenceController {

    private final DocumentSequenceService sequenceService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "document-sequences");
        return "setup/doc-sequence-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return sequenceService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", sequenceService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid DocumentSequenceDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { sequenceService.update(dto.getId(), dto); res.put("message", "Sequence updated successfully."); }
            else                     { sequenceService.create(dto);              res.put("message", "Sequence created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { sequenceService.delete(id); res.put("success", true); res.put("message", "Sequence deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /**
     * Generate next document number — called by transaction modules.
     * POST /document-sequences/next
     * Body: { prefix, yearCode }
     */
    @PostMapping("/next")
    @ResponseBody
    public Map<String, Object> next(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long orgId = SecurityHelper.requireOrgId();
            String docNumber = sequenceService.nextDocumentNumber(
                    orgId,
                    body.get("prefix"),
                    body.get("yearCode"));
            res.put("success", true);
            res.put("docNumber", docNumber);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Sequences for the current org — for admin overview */
    @GetMapping("/by-org")
    @ResponseBody
    public List<Map<String, Object>> byOrg() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        if (orgId == null) return List.of();
        return sequenceService.findByOrg(orgId).stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       s.getId());
            m.put("prefix",   s.getPrefix());
            m.put("yearCode", s.getYearCode());
            m.put("lastSeq",  s.getLastSeq());
            return m;
        }).toList();
    }
}
