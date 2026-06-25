package com.asg.spindleserp.approval.controller;

import com.asg.spindleserp.approval.dto.*;
import com.asg.spindleserp.approval.service.ApprovalService;
import com.asg.spindleserp.common.dto.DataTableResponse;
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
 * ApprovalController — all approval module pages and REST endpoints.
 *
 * Pages:
 *   GET /approval/dashboard      → approval/approval-dashboard.html   ← NEW
 *   GET /approval/configs        → approval/approval-configs.html
 *   GET /approval/inbox          → approval/approval-inbox.html
 *   GET /approval/requests       → approval/approval-requests.html
 *   GET /approval/delegations    → approval/approval-delegations.html
 *
 * Dashboard API:
 *   GET /approval/dashboard/summary → rich JSON payload
 *
 * JS prefixes:
 *   cfg* | inbox* | req* | del*
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    // ── Pages ──────────────────────────────────────────────────────────────────

    @GetMapping("/approval/dashboard")
    public String dashboardPage(Model m) {
        m.addAttribute("activePage", "approval-dashboard");
        return "approval/approval-dashboard";
    }

    @GetMapping("/approval/configs")
    public String configPage(Model m) { m.addAttribute("activePage","approval-configs"); return "approval/approval-configs"; }

    @GetMapping("/approval/inbox")
    public String inboxPage(Model m) { m.addAttribute("activePage","approval-inbox"); return "approval/approval-inbox"; }

    @GetMapping("/approval/requests")
    public String requestsPage(Model m) { m.addAttribute("activePage","approval-requests"); return "approval/approval-requests"; }

    @GetMapping("/approval/delegations")
    public String delegationsPage(Model m) { m.addAttribute("activePage","approval-delegations"); return "approval/approval-delegations"; }

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping("/approval/dashboard/summary")
    @ResponseBody
    public Map<String, Object> summary() { return approvalService.dashboardSummary(); }

    // ── also keep the old /apr/inbox redirect used by ERP sidebar ─────────────
    @GetMapping("/apr/inbox")
    public String aprInboxRedirect() { return "redirect:/approval/inbox"; }

    @GetMapping("/apr/configs")
    public String aprConfigsRedirect() { return "redirect:/approval/configs"; }

    // ── Config ─────────────────────────────────────────────────────────────────

    @GetMapping("/approval/configs/list")
    @ResponseBody
    public DataTableResponse cfgList(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search) {
        return approvalService.configDatatable(draw, start, length, search);
    }

    @GetMapping("/approval/configs/show/{id}")
    @ResponseBody
    public Map<String,Object> cfgShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", approvalService.findConfigById(id)));
    }

    @PostMapping("/approval/configs/save")
    @ResponseBody
    public Map<String,Object> cfgSave(@RequestBody @Valid ApprovalConfigDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { approvalService.updateConfig(dto.getId(), dto); return "Config updated."; }
            else { approvalService.createConfig(dto); return "Config created."; }
        });
    }

    @PostMapping("/approval/configs/toggle/{id}")
    @ResponseBody
    public Map<String,Object> cfgToggle(@PathVariable Long id) {
        return ok(() -> { approvalService.toggleConfig(id); return "Config status toggled."; });
    }

    @DeleteMapping("/approval/configs/delete/{id}")
    @ResponseBody
    public Map<String,Object> cfgDelete(@PathVariable Long id) {
        return ok(() -> { approvalService.deleteConfig(id); return "Config deleted."; });
    }

    @GetMapping("/approval/configs/search")
    @ResponseBody
    public Map<String,Object> cfgSearch(@RequestParam(defaultValue="") String search, @RequestParam(defaultValue="1") int page) {
        return approvalService.searchConfigs(search, page);
    }

    // ── Inbox ──────────────────────────────────────────────────────────────────

    @GetMapping("/approval/inbox/list")
    @ResponseBody
    public DataTableResponse inboxList(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search) {
        return approvalService.inboxDatatable(draw, start, length, search);
    }

    @GetMapping("/approval/inbox/show/{id}")
    @ResponseBody
    public Map<String,Object> inboxShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", approvalService.findRequestById(id)));
    }

    @PostMapping("/approval/inbox/approve/{id}")
    @ResponseBody
    public Map<String,Object> inboxApprove(@PathVariable Long id, @RequestParam(required=false) String comments) {
        return ok(() -> { approvalService.approve(id, comments); return "Request approved successfully."; });
    }

    @PostMapping("/approval/inbox/reject/{id}")
    @ResponseBody
    public Map<String,Object> inboxReject(@PathVariable Long id, @RequestParam(required=false) String reason) {
        return ok(() -> { approvalService.reject(id, reason); return "Request rejected."; });
    }

    @PostMapping("/approval/inbox/return/{id}")
    @ResponseBody
    public Map<String,Object> inboxReturn(@PathVariable Long id, @RequestParam(required=false) String reason) {
        return ok(() -> { approvalService.returnForCorrection(id, reason); return "Request returned for correction."; });
    }

    @PostMapping("/approval/inbox/hold/{id}")
    @ResponseBody
    public Map<String,Object> inboxHold(@PathVariable Long id, @RequestParam(required=false) String comments) {
        return ok(() -> { approvalService.hold(id, comments); return "Request placed on hold."; });
    }

    @PostMapping("/approval/inbox/release/{id}")
    @ResponseBody
    public Map<String,Object> inboxRelease(@PathVariable Long id) {
        return ok(() -> { approvalService.release(id); return "Request released from hold."; });
    }

    // ── Requests (admin view) ──────────────────────────────────────────────────

    @GetMapping("/approval/requests/list")
    @ResponseBody
    public DataTableResponse reqList(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(defaultValue="") String status) {
        return approvalService.requestDatatable(draw, start, length, search, status);
    }

    @GetMapping("/approval/requests/show/{id}")
    @ResponseBody
    public Map<String,Object> reqShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", approvalService.findRequestById(id)));
    }

    @PostMapping("/approval/requests/submit")
    @ResponseBody
    public Map<String,Object> reqSubmit(@RequestBody @Valid ApprovalRequestDTO dto) {
        return ok(() -> { ApprovalRequestDTO saved = approvalService.submitRequest(dto); return "Request " + saved.getReferenceNumber() + " submitted."; });
    }

    @PostMapping("/approval/requests/cancel/{id}")
    @ResponseBody
    public Map<String,Object> reqCancel(@PathVariable Long id, @RequestParam(required=false) String reason) {
        return ok(() -> { approvalService.cancelRequest(id, reason); return "Request cancelled."; });
    }

    @GetMapping("/approval/requests/history/{id}")
    @ResponseBody
    public List<ApprovalRequestDTO.HistoryDTO> reqHistory(@PathVariable Long id) {
        return approvalService.getHistory(id);
    }

    @GetMapping("/approval/requests/by-reference")
    @ResponseBody
    public Map<String,Object> reqByRef(@RequestParam Long refId, @RequestParam String docType) {
        return ok(() -> Map.of("defaultData", approvalService.findRequestByReference(refId, docType)));
    }

    // ── Delegations ────────────────────────────────────────────────────────────

    @GetMapping("/approval/delegations/list")
    @ResponseBody
    public DataTableResponse delList(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search) {
        return approvalService.delegationDatatable(draw, start, length, search);
    }

    @GetMapping("/approval/delegations/show/{id}")
    @ResponseBody
    public Map<String,Object> delShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", approvalService.findDelegationById(id)));
    }

    @PostMapping("/approval/delegations/save")
    @ResponseBody
    public Map<String,Object> delSave(@RequestBody @Valid ApprovalDelegationDTO dto) {
        return ok(() -> { approvalService.createDelegation(dto); return "Delegation created."; });
    }

    @PostMapping("/approval/delegations/revoke/{id}")
    @ResponseBody
    public Map<String,Object> delRevoke(@PathVariable Long id, @RequestParam(required=false) String reason) {
        return ok(() -> { approvalService.revokeDelegation(id, reason); return "Delegation revoked."; });
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String,Object> ok(Checked action) {
        Map<String,Object> res = new HashMap<>();
        try {
            Object r = action.run();
            res.put("success", true);
            if (r instanceof String msg) res.put("message", msg);
            else if (r instanceof Map<?,?> m) res.put("obj", m);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @FunctionalInterface interface Checked { Object run() throws Exception; }
}
