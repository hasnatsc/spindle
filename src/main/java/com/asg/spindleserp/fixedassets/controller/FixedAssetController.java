package com.asg.spindleserp.fixedassets.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.fixedassets.dto.*;
import com.asg.spindleserp.fixedassets.service.FixedAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FixedAssetController
 *
 * Pages:
 *   GET /fixed-assets/categories          → fa-categories.html
 *   GET /fixed-assets/assets              → fa-assets.html
 *   GET /fixed-assets/depreciation        → fa-depreciation.html
 *   GET /fixed-assets/disposals           → fa-disposals.html
 *
 * REST:
 *   Category:
 *     GET    /fixed-assets/categories/list
 *     GET    /fixed-assets/categories/show/{id}
 *     POST   /fixed-assets/categories/save
 *     POST   /fixed-assets/categories/toggle/{id}
 *     DELETE /fixed-assets/categories/delete/{id}
 *     GET    /fixed-assets/categories/search
 *
 *   Asset:
 *     GET    /fixed-assets/assets/list?status=
 *     GET    /fixed-assets/assets/show/{id}
 *     POST   /fixed-assets/assets/save
 *     DELETE /fixed-assets/assets/delete/{id}
 *     GET    /fixed-assets/assets/search
 *
 *   Depreciation Run:
 *     GET    /fixed-assets/depreciation/list
 *     GET    /fixed-assets/depreciation/show/{id}
 *     POST   /fixed-assets/depreciation/calculate
 *     POST   /fixed-assets/depreciation/post/{id}
 *     POST   /fixed-assets/depreciation/reverse/{id}
 *
 *   Disposal:
 *     GET    /fixed-assets/disposals/list
 *     GET    /fixed-assets/disposals/by-asset/{assetId}
 *     POST   /fixed-assets/disposals/save
 *
 * JS prefixes:
 *   Category: cat*
 *   Asset:    asset*
 *   Dep Run:  run*
 *   Disposal: disp*
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService faService;

    // ── Pages ──────────────────────────────────────────────────────────────────

    @GetMapping("/fixed-assets/categories")
    public String categoriesPage(Model model) {
        model.addAttribute("activePage", "fa-categories");
        return "fixed-assets/fa-categories";
    }

    @GetMapping("/fixed-assets/assets")
    public String assetsPage(Model model) {
        model.addAttribute("activePage", "fa-assets");
        return "fixed-assets/fa-assets";
    }

    @GetMapping("/fixed-assets/depreciation")
    public String depreciationPage(Model model) {
        model.addAttribute("activePage", "fa-depreciation");
        return "fixed-assets/fa-depreciation";
    }

    @GetMapping("/fixed-assets/disposals")
    public String disposalsPage(Model model) {
        model.addAttribute("activePage", "fa-disposals");
        return "fixed-assets/fa-disposals";
    }

    // ── Asset Category ─────────────────────────────────────────────────────────

    @GetMapping("/fixed-assets/categories/list")
    @ResponseBody
    public DataTableResponse categoryList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return faService.categoryDatatable(draw, start, length, search);
    }

    @GetMapping("/fixed-assets/categories/show/{id}")
    @ResponseBody
    public Map<String, Object> categoryShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", faService.findCategoryById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/categories/save")
    @ResponseBody
    public Map<String, Object> categorySave(@RequestBody @Valid AssetCategoryDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { faService.updateCategory(dto.getId(), dto); res.put("message", "Category updated."); }
            else                     { faService.createCategory(dto);               res.put("message", "Category created."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/categories/toggle/{id}")
    @ResponseBody
    public Map<String, Object> categoryToggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            AssetCategoryDTO dto = faService.toggleCategoryStatus(id);
            res.put("success", true);
            res.put("message", "Category " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + ".");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/fixed-assets/categories/delete/{id}")
    @ResponseBody
    public Map<String, Object> categoryDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { faService.deleteCategory(id); res.put("success", true); res.put("message", "Category deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/fixed-assets/categories/search")
    @ResponseBody
    public Map<String, Object> categorySearch(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "1")  int    page) {
        return faService.searchCategories(search, page);
    }

    // ── Asset Master ───────────────────────────────────────────────────────────

    @GetMapping("/fixed-assets/assets/list")
    @ResponseBody
    public DataTableResponse assetList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "")   String status) {
        return faService.assetDatatable(draw, start, length, search, status);
    }

    @GetMapping("/fixed-assets/assets/show/{id}")
    @ResponseBody
    public Map<String, Object> assetShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", faService.findAssetById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/assets/save")
    @ResponseBody
    public Map<String, Object> assetSave(@RequestBody @Valid AssetDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            AssetDTO saved;
            if (dto.getId() != null) { saved = faService.updateAsset(dto.getId(), dto); res.put("message", "Asset updated."); }
            else                     { saved = faService.createAsset(dto);               res.put("message", "Asset registered."); }
            res.put("success", true);
            res.put("id", saved.getId());
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/fixed-assets/assets/delete/{id}")
    @ResponseBody
    public Map<String, Object> assetDelete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { faService.deleteAsset(id); res.put("success", true); res.put("message", "Asset deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/fixed-assets/assets/search")
    @ResponseBody
    public Map<String, Object> assetSearch(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "1")  int    page) {
        return faService.searchAssets(search, page);
    }

    // ── Depreciation Run ───────────────────────────────────────────────────────

    @GetMapping("/fixed-assets/depreciation/list")
    @ResponseBody
    public DataTableResponse runList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return faService.runDatatable(draw, start, length, search);
    }

    @GetMapping("/fixed-assets/depreciation/show/{id}")
    @ResponseBody
    public Map<String, Object> runShow(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", faService.findRunById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/depreciation/calculate")
    @ResponseBody
    public Map<String, Object> runCalculate(@RequestBody @Valid DepreciationRunDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            DepreciationRunDTO result = faService.calculateDepreciation(dto);
            res.put("success", true);
            res.put("message", "Depreciation calculated for " + result.getTotalAssets() + " assets. Total: " + result.getTotalDepreciation());
            res.put("id", result.getId());
            res.put("obj", Map.of("defaultData", result));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/depreciation/post/{id}")
    @ResponseBody
    public Map<String, Object> runPost(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            DepreciationRunDTO dto = faService.postDepreciationRun(id);
            res.put("success", true);
            res.put("message", "Depreciation run posted. GL journal created.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/depreciation/reverse/{id}")
    @ResponseBody
    public Map<String, Object> runReverse(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            faService.reverseDepreciationRun(id);
            res.put("success", true);
            res.put("message", "Depreciation run reversed. Asset balances restored.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Asset Disposal ─────────────────────────────────────────────────────────

    @GetMapping("/fixed-assets/disposals/list")
    @ResponseBody
    public DataTableResponse disposalList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return faService.disposalDatatable(draw, start, length, search);
    }

    @GetMapping("/fixed-assets/disposals/by-asset/{assetId}")
    @ResponseBody
    public Map<String, Object> disposalByAsset(@PathVariable Long assetId) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", faService.findDisposalByAsset(assetId))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/fixed-assets/disposals/show/{id}")
    @ResponseBody
    public Map<String, Object> disposalShow(@PathVariable Long id) {
        // Disposal list only shows by-asset, but keep show endpoint for direct access
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("message", "Use by-asset endpoint."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/fixed-assets/disposals/save")
    @ResponseBody
    public Map<String, Object> disposalSave(@RequestBody @Valid AssetDisposalDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            AssetDisposalDTO saved = faService.disposeAsset(dto);
            res.put("success", true);
            res.put("message", "Asset disposed successfully. GL journal created.");
            res.put("id", saved.getId());
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
