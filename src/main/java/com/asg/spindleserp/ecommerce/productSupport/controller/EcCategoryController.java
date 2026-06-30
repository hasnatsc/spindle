// Path: com/asg/spindleserp/ecommerce/controller/EcCategoryController.java
package com.asg.spindleserp.ecommerce.productSupport.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.productSupport.dto.EcCategoryDTO;
import com.asg.spindleserp.ecommerce.productSupport.service.EcCategoryService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * EcCategoryController  /ecommerce/categories
 * JS fns: eccShow / eccEdit / eccToggle / eccDelete / eccOpenCreate
 */
@Controller
@RequestMapping("/ecommerce/categories")
@RequiredArgsConstructor
public class EcCategoryController {

    private final EcCategoryService categoryService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "ec-categories");
        return "ecommerce/productSupport/ec-category-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,
            @RequestParam(value="search[value]", defaultValue="") String search) {
        return categoryService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", categoryService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid EcCategoryDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { categoryService.update(dto.getId(), dto); res.put("message", "Category updated."); }
            else                     { categoryService.create(dto);              res.put("message", "Category created."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCategoryDTO d = categoryService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Category " + (Boolean.TRUE.equals(d.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { categoryService.delete(id); res.put("success", true); res.put("message", "Category deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Active categories for product catalog dropdown */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.requireOrgId();
        return categoryService.findActiveByOrg(orgId).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   c.getId());
            m.put("code", c.getCategoryCode());
            m.put("name", c.getCategoryName());
            m.put("slug", c.getSlug());
            return m;
        }).toList();
    }

    /** Category attribute schema — used by product catalog attribute tab */
    @GetMapping("/{id}/attributes")
    @ResponseBody
    public List<Map<String, Object>> attributes(@PathVariable Long id) {
        EcCategoryDTO dto = categoryService.findById(id);
        return dto.getAttributes().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             a.getId());
            m.put("attributeName",  a.getAttributeName());
            m.put("attributeLabel", a.getAttributeLabel());
            m.put("dataType",       a.getDataType());
            m.put("isRequired",     a.getIsRequired());
            return m;
        }).toList();
    }
}
