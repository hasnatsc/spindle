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

    // ── Payment Mode → Account mapping ─────────────────────────────────────────

    @GetMapping("/settings/payment-mode-accounts")
    @ResponseBody
    public Map<String, Object> getPaymentModeAccounts() {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("data", settingsService.getPaymentModeAccounts()); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /**
     * Body: { paymentMode: "BKASH", subAccountId: 123 }
     * Upserts the mapping for this org + payment mode.
     */
    @PostMapping("/settings/payment-mode-accounts")
    @ResponseBody
    public Map<String, Object> savePaymentModeAccount(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String mode = (String) body.get("paymentMode");
            Number subId = (Number) body.get("subAccountId");
            settingsService.savePaymentModeAccount(mode, subId != null ? subId.longValue() : null);
            res.put("success", true);
            res.put("message", "Payment mode account saved.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
