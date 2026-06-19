package com.asg.spindleserp.inventory.transaction.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.transaction.dto.StockAdjustmentDTO;
import com.asg.spindleserp.inventory.transaction.service.StockMovementService;
import com.asg.spindleserp.security.auth.ContextProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StockAdjustmentController
 *
 * URL: GET /inventory/adjustments
 *
 * JS fn → endpoint:
 *   adjShow(id)    GET    /inventory/adjustments/show/{id}
 *   adjEdit(id)    GET    /inventory/adjustments/show/{id}
 *   adjDelete(id)  DELETE /inventory/adjustments/delete/{id}
 *   (save)         POST   /inventory/adjustments/save
 *   (confirm)      POST   /inventory/adjustments/confirm/{id}
 *   (cancel)       POST   /inventory/adjustments/cancel/{id}
 */
@Slf4j
@Controller
@RequestMapping("/inventory/adjustments")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockMovementService movementService;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "stock-adjustments");
        return "inventory/transaction/adjustment-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return movementService.adjustmentDatatableList(draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", movementService.findAdjustmentById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Save (create / update) ────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid StockAdjustmentDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            StockAdjustmentDTO saved;
            dto.setWarehouseId(ContextProvider.getWarehouseId());
            if (dto.getId() != null) {
                saved = movementService.updateAdjustment(dto.getId(), dto);
                res.put("message", "Adjustment updated successfully.");
            } else {
                saved = movementService.createAdjustment(dto);
                res.put("message", "Adjustment " + saved.getDocumentNo() + " created successfully.");
            }
            res.put("success", true);
            res.put("documentNo", saved.getDocumentNo());
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Confirm (post stock) ──────────────────────────────────────────────────

    @PostMapping("/confirm/{id}")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            StockAdjustmentDTO dto = movementService.confirmAdjustment(id);
            res.put("success", true);
            res.put("message", "Adjustment " + dto.getDocumentNo() + " confirmed and stock posted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public Map<String, Object> cancel(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            movementService.cancelAdjustment(id);
            res.put("success", true);
            res.put("message", "Adjustment cancelled.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            movementService.deleteAdjustment(id);
            res.put("success", true);
            res.put("message", "Adjustment deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
