package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvVisaApplicationDTO;
import com.asg.spindleserp.travel.dto.TrvVisaTypeDTO;
import com.asg.spindleserp.travel.service.TravelVisaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelVisaController — visa applications (with document checklist) and
 * the visa-type lookup table. JS prefixes: visa* (application), vt* (visa type).
 */
@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelVisaController {

    private final TravelVisaService visaService;

    @GetMapping("/visa-applications")
    public String visaPage(Model model) {
        model.addAttribute("activePage", "travel-visa");
        model.addAttribute("pageTitle",  "Visa Applications");
        return "travel/travel-visa-applications";
    }

    // ── VISA TYPES ────────────────────────────────────────────────────────────

    @GetMapping("/visa-applications/types/list")
    @ResponseBody
    public Map<String, Object> typeList() { return Map.of("data", visaService.listVisaTypes()); }

    @PostMapping("/visa-applications/types/save")
    @ResponseBody
    public Map<String, Object> typeSave(@RequestBody @Valid TrvVisaTypeDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { visaService.saveVisaType(dto); res.put("success", true); res.put("message", "Visa type saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/visa-applications/types/delete/{id}")
    @ResponseBody
    public Map<String, Object> typeDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { visaService.deleteVisaType(id); res.put("success", true); res.put("message", "Visa type deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── VISA APPLICATIONS ─────────────────────────────────────────────────────

    @GetMapping("/visa-applications/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam(defaultValue = "") String search) {
        return Map.of("data", visaService.listApplications(search));
    }

    @GetMapping("/visa-applications/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", visaService.findApplicationById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/visa-applications/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid TrvVisaApplicationDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvVisaApplicationDTO saved = visaService.saveApplication(dto);
            res.put("success", true); res.put("id", saved.getId());
            res.put("message", dto.getId() != null ? "Visa application updated." : "Visa application created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/visa-applications/status/{id}")
    @ResponseBody
    public Map<String, Object> status(@PathVariable Long id, @RequestParam String status) {
        Map<String, Object> res = new HashMap<>();
        try { visaService.changeApplicationStatus(id, status); res.put("success", true); res.put("message", "Status updated to " + status + "."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/visa-applications/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { visaService.deleteApplication(id); res.put("success", true); res.put("message", "Visa application deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
