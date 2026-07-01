package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvGlAccountDefaultsDTO;
import com.asg.spindleserp.travel.service.TravelSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelSettingsController {

    private final TravelSettingsService settingsService;

    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("activePage", "travel-settings");
        model.addAttribute("pageTitle",  "Travel Settings");
        return "travel/travel-settings";
    }

    @GetMapping("/settings/defaults")
    @ResponseBody
    public Map<String, Object> getDefaults() {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", settingsService.getDefaults())); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/settings/defaults")
    @ResponseBody
    public Map<String, Object> saveDefaults(@RequestBody @Valid TrvGlAccountDefaultsDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try { settingsService.saveDefaults(dto); res.put("success", true); res.put("message", "GL defaults saved."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
