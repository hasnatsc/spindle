// Path: com/asg/spindleserp/ecommerce/controller/EcProductCatalogController.java
package com.asg.spindleserp.ecommerce.productSupport.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.productSupport.dto.EcProductCatalogDTO;
import com.asg.spindleserp.ecommerce.productSupport.service.EcProductCatalogService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EcProductCatalogController  /ecommerce/products
 * JS fns: ecpShow / ecpEdit / ecpToggle / ecpTogglePublish / ecpDelete / ecpOpenCreate
 */
@Slf4j
@Controller
@RequestMapping("/ecommerce/products")
@RequiredArgsConstructor
public class EcProductCatalogController {

    private final EcProductCatalogService catalogService;

    // ── Page ──────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "ec-products");
        return "ecommerce/productSupport/ec-product-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return catalogService.datatableList(draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", catalogService.findById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Save (create + update) ────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid EcProductCatalogDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                catalogService.update(dto.getId(), dto);
                res.put("message", "Product updated successfully.");
            } else {
                catalogService.create(dto);
                res.put("message", "Product created successfully.");
            }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Toggle active/inactive ────────────────────────────────────────────────

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcProductCatalogDTO dto = catalogService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Product " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Toggle published / draft ──────────────────────────────────────────────

    @PostMapping("/toggle-publish/{id}")
    @ResponseBody
    public Map<String, Object> togglePublish(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcProductCatalogDTO dto = catalogService.togglePublished(id);
            res.put("success", true);
            res.put("message", "Product " + (Boolean.TRUE.equals(dto.getPublished()) ? "published" : "set to draft") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            catalogService.delete(id);
            res.put("success", true);
            res.put("message", "Product deleted successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Lookup: AJAX Select2 search ───────────────────────────────────────────

    /**
     * GET /ecommerce/products/search?search=&page=&pageSize=
     * Returns: { items: [{id, text, slug, title, itemCode}], hasMore }
     */
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "1")  int    page,
            @RequestParam(defaultValue = "30") int    pageSize) {

        Long orgId = SecurityHelper.requireOrgId();
        List<EcProductCatalogDTO> all = catalogService.findActiveByOrg(orgId);

        String q = search.trim().toLowerCase();
        List<EcProductCatalogDTO> filtered = q.isEmpty() ? all
                : all.stream()
                    .filter(p -> (p.getProductTitle() != null && p.getProductTitle().toLowerCase().contains(q))
                              || (p.getSlug()         != null && p.getSlug().toLowerCase().contains(q))
                              || (p.getItemCode()     != null && p.getItemCode().toLowerCase().contains(q))
                              || (p.getItemName()     != null && p.getItemName().toLowerCase().contains(q)))
                    .collect(Collectors.toList());

        int  from    = (page - 1) * pageSize;
        int  to      = Math.min(from + pageSize, filtered.size());
        boolean hasMore = to < filtered.size();
        List<EcProductCatalogDTO> paged = (from >= filtered.size()) ? List.of() : filtered.subList(from, to);

        List<Map<String, Object>> items = paged.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        p.getId());
            m.put("text",      (p.getItemCode() != null ? p.getItemCode() : "") + " — " + p.getProductTitle());
            m.put("slug",      p.getSlug());
            m.put("title",     p.getProductTitle());
            m.put("itemCode",  p.getItemCode());
            m.put("published", p.getPublished());
            return m;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items",   items);
        res.put("hasMore", hasMore);
        return res;
    }

    /** Active published products — for storefront / order modules */
    @GetMapping("/published")
    @ResponseBody
    public List<Map<String, Object>> published() {
        Long orgId = SecurityHelper.requireOrgId();
        return catalogService.findActiveByOrg(orgId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",        p.getId());
                    m.put("title",     p.getProductTitle());
                    m.put("slug",      p.getSlug());
                    m.put("itemCode",  p.getItemCode());
                    m.put("unitPrice", p.getItemUnitPrice());
                    return m;
                }).toList();
    }
}
