package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.CountryDTO;
import com.asg.spindleserp.setup.service.CountryService;
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
 * CountryController  /countries
 *
 * JS fn → endpoint:
 *   countryShow(id)   GET    /countries/show/{id}
 *   countryEdit(id)   GET    /countries/show/{id}
 *   countryToggle(id) POST   /countries/toggle/{id}
 *   countryDelete(id) DELETE /countries/delete/{id}
 *   (save)            POST   /countries/save
 */
@Slf4j
@Controller
@RequestMapping("/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "countries");
        return "setup/country-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return countryService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", countryService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid CountryDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { countryService.update(dto.getId(), dto); res.put("message", "Country updated successfully."); }
            else                     { countryService.create(dto);              res.put("message", "Country created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            CountryDTO dto = countryService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Country " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { countryService.delete(id); res.put("success", true); res.put("message", "Country deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Dropdown for address / LC country pickers */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        return countryService.findActive().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",       c.getId());
                    m.put("isoCode",  c.getIsoCode());
                    m.put("isoCode2", c.getIsoCode2());
                    m.put("name",     c.getName());
                    m.put("phoneCode",c.getPhoneCode() != null ? c.getPhoneCode() : "");
                    return m;
                }).toList();
    }
}
