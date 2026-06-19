package com.asg.spindleserp.inventory.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.dto.ItemDTO;
import com.asg.spindleserp.inventory.service.ItemService;
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
 * ItemController  /items
 * JS fns: itemShow / itemEdit / itemToggle / itemDelete / itemOpenCreate
 *
 * NEW: GET /items/search?search=&page=&pageSize=   → {items:[{id,text,code,name,uom}], hasMore}
 *      Used by AJAX Select2 on adjustment, transfer, stock-balance, stock-ledger pages.
 */
@Slf4j
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "items");
        return "inventory/item-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return itemService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", itemService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ItemDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { itemService.update(dto.getId(), dto); res.put("message", "Item updated successfully."); }
            else                     { itemService.create(dto);              res.put("message", "Item created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ItemDTO dto = itemService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Item " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { itemService.delete(id); res.put("success", true); res.put("message", "Item deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Lookup endpoints ──────────────────────────────────────────────────────

    /** All active items for current org (flat list — legacy use) */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.requireOrgId();
        return itemService.findActiveByOrg(orgId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       i.getId());
            m.put("code",     i.getItemCode());
            m.put("name",     i.getItemName());
            m.put("itemType", i.getItemType());
            m.put("uom",      i.getUnitOfMeasure());
            return m;
        }).toList();
    }

    /**
     * AJAX Select2 search endpoint.
     * Returns: { items: [{id, text, code, name, uom, itemType}], hasMore }
     *
     * text = "{code} — {name}" — ready for Select2 templateSelection.
     * Supports pagination: page (1-based), pageSize (default 30).
     * Search is applied against itemCode + itemName (case-insensitive contains).
     */
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "1")  int    page,
            @RequestParam(defaultValue = "30") int    pageSize) {

        Long orgId = SecurityHelper.requireOrgId();

        List<ItemDTO> all = itemService.findActiveByOrg(orgId);

        // In-memory filter (fast enough; org item sets are small)
        String q = search.trim().toLowerCase();
        List<ItemDTO> filtered = q.isEmpty() ? all
                : all.stream()
                    .filter(i -> (i.getItemCode() != null && i.getItemCode().toLowerCase().contains(q)) || (i.getItemName() != null && i.getItemName().toLowerCase().contains(q)))
                    .collect(Collectors.toList());

        // Paginate
        int from    = (page - 1) * pageSize;
        int to      = Math.min(from + pageSize, filtered.size());
        boolean hasMore = to < filtered.size();
        List<ItemDTO> paged = (from >= filtered.size()) ? List.of() : filtered.subList(from, to);

        List<Map<String, Object>> items = paged.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       i.getId());
            m.put("text",     (i.getItemCode() != null ? i.getItemCode() : "") + " — " + (i.getItemName() != null ? i.getItemName() : ""));
            m.put("code",     i.getItemCode());
            m.put("name",     i.getItemName());
            m.put("uom",      i.getUnitOfMeasure());
            m.put("itemType", i.getItemType());
            return m;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items",   items);
        res.put("hasMore", hasMore);
        return res;
    }

    /** Production BOM inputs: RAW_MATERIAL, SEMI_FINISHED, CONSUMABLE, MRO */
    @GetMapping("/production-inputs")
    @ResponseBody
    public List<Map<String, Object>> productionInputs() {
        Long orgId = SecurityHelper.requireOrgId();
        return itemService.findProductionInputs(orgId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       i.getId());
            m.put("code",     i.getItemCode());
            m.put("name",     i.getItemName());
            m.put("itemType", i.getItemType());
            m.put("uom",      i.getUnitOfMeasure());
            return m;
        }).toList();
    }

    /** FINISHED_GOOD and SEMI_FINISHED — for sales / production output */
    @GetMapping("/finished-goods")
    @ResponseBody
    public List<Map<String, Object>> finishedGoods() {
        Long orgId = SecurityHelper.requireOrgId();
        return itemService.findFinishedGoods(orgId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        i.getId());
            m.put("code",      i.getItemCode());
            m.put("name",      i.getItemName());
            m.put("uom",       i.getUnitOfMeasure());
            m.put("unitPrice", i.getUnitPrice());
            return m;
        }).toList();
    }

    /** ItemType enum values */
    @GetMapping("/item-types")
    @ResponseBody
    public List<String> itemTypes() {
        return Arrays.stream(ItemType.values()).map(Enum::name).toList();
    }
}
