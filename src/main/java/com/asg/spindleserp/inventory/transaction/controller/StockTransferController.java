package com.asg.spindleserp.inventory.transaction.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.transaction.dto.StockTransferDTO;
import com.asg.spindleserp.inventory.transaction.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StockTransferController
 *
 * URL: GET /inventory/transfers
 *
 * JS fn → endpoint:
 *   trShow(id)    GET    /inventory/transfers/show/{id}
 *   trEdit(id)    GET    /inventory/transfers/show/{id}
 *   trDelete(id)  DELETE /inventory/transfers/delete/{id}
 *   (save)        POST   /inventory/transfers/save
 *   (confirm)     POST   /inventory/transfers/confirm/{id}
 *   (cancel)      POST   /inventory/transfers/cancel/{id}
 */
@Slf4j
@Controller
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockMovementService movementService;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "stock-transfers");
        return "inventory/transfer-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return movementService.transferDatatableList(draw, start, length, search);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", Map.of("defaultData", movementService.findTransferById(id)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Save (create / update) ────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid StockTransferDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            StockTransferDTO saved;
            if (dto.getId() != null) {
                saved = movementService.updateTransfer(dto.getId(), dto);
                res.put("message", "Transfer updated successfully.");
            } else {
                saved = movementService.createTransfer(dto);
                res.put("message", "Transfer " + saved.getDocumentNo() + " created successfully.");
            }
            res.put("success", true);
            res.put("documentNo", saved.getDocumentNo());
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

    @PostMapping("/confirm/{id}")
    @ResponseBody
    public Map<String, Object> confirm(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            StockTransferDTO dto = movementService.confirmTransfer(id);
            res.put("success", true);
            res.put("message", "Transfer " + dto.getDocumentNo() + " confirmed. Stock moved.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public Map<String, Object> cancel(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            movementService.cancelTransfer(id);
            res.put("success", true);
            res.put("message", "Transfer cancelled.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            movementService.deleteTransfer(id);
            res.put("success", true);
            res.put("message", "Transfer deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
