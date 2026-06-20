package com.asg.spindleserp.crm.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.crm.dto.*;
import com.asg.spindleserp.crm.service.CrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CrmController — all CRM entities in one controller.
 *
 * Pages:
 *   GET /crm/leads              → crm-leads.html
 *   GET /crm/opportunities      → crm-opportunities.html
 *   GET /crm/contacts           → crm-contacts.html
 *   GET /crm/activities         → crm-activities.html
 *   GET /crm/feedback           → crm-feedback.html
 *
 * REST:
 *   Leads:         /crm/leads/*
 *   Opportunities: /crm/opportunities/*
 *   Contacts:      /crm/contacts/*
 *   Activities:    /crm/activities/*
 *   Feedback:      /crm/feedback/*
 *   Dashboard:     /crm/dashboard/summary
 *
 * JS prefixes:
 *   lead* / opp* / ct* / act* / fb*
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CrmController {

    private final CrmService crmService;

    // ── Pages ──────────────────────────────────────────────────────────────────

    @GetMapping("/crm/leads")
    public String leadsPage(Model model) {
        model.addAttribute("activePage", "crm-leads");
        return "crm/crm-leads";
    }

    @GetMapping("/crm/opportunities")
    public String opportunitiesPage(Model model) {
        model.addAttribute("activePage", "crm-opportunities");
        return "crm/crm-opportunities";
    }

    @GetMapping("/crm/contacts")
    public String contactsPage(Model model) {
        model.addAttribute("activePage", "crm-contacts");
        return "crm/crm-contacts";
    }

    @GetMapping("/crm/activities")
    public String activitiesPage(Model model) {
        model.addAttribute("activePage", "crm-activities");
        return "crm/crm-activities";
    }

    @GetMapping("/crm/feedback")
    public String feedbackPage(Model model) {
        model.addAttribute("activePage", "crm-feedback");
        return "crm/crm-feedback";
    }

    // ── Dashboard summary ──────────────────────────────────────────────────────

    @GetMapping("/crm/dashboard/summary")
    @ResponseBody
    public Map<String, Object> dashboardSummary() { return crmService.dashboardSummary(); }

    // ── LEADS ─────────────────────────────────────────────────────────────────

    @GetMapping("/crm/leads/list")
    @ResponseBody
    public DataTableResponse leadList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "")   String status) {
        return crmService.leadDatatable(draw, start, length, search, status);
    }

    @GetMapping("/crm/leads/show/{id}")
    @ResponseBody
    public Map<String, Object> leadShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", crmService.findLeadById(id)));
    }

    @PostMapping("/crm/leads/save")
    @ResponseBody
    public Map<String, Object> leadSave(@RequestBody @Valid LeadDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { crmService.updateLead(dto.getId(), dto); return "Lead updated."; }
            else { crmService.createLead(dto); return "Lead created."; }
        });
    }

    @PostMapping("/crm/leads/status/{id}")
    @ResponseBody
    public Map<String, Object> leadStatus(@PathVariable Long id, @RequestParam String status) {
        return ok(() -> { crmService.updateLeadStatus(id, status); return "Status updated to " + status + "."; });
    }

    @DeleteMapping("/crm/leads/delete/{id}")
    @ResponseBody
    public Map<String, Object> leadDelete(@PathVariable Long id) {
        return ok(() -> { crmService.deleteLead(id); return "Lead deleted."; });
    }

    @GetMapping("/crm/leads/search")
    @ResponseBody
    public Map<String, Object> leadSearch(
            @RequestParam(defaultValue = "")  String search,
            @RequestParam(defaultValue = "1") int    page) {
        return crmService.searchLeads(search, page);
    }

    // ── OPPORTUNITIES ──────────────────────────────────────────────────────────

    @GetMapping("/crm/opportunities/list")
    @ResponseBody
    public DataTableResponse oppList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "")   String stage) {
        return crmService.opportunityDatatable(draw, start, length, search, stage);
    }

    @GetMapping("/crm/opportunities/show/{id}")
    @ResponseBody
    public Map<String, Object> oppShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", crmService.findOpportunityById(id)));
    }

    @PostMapping("/crm/opportunities/save")
    @ResponseBody
    public Map<String, Object> oppSave(@RequestBody @Valid OpportunityDTO dto) {
        return ok(() -> {
            OpportunityDTO saved;
            if (dto.getId() != null) { saved = crmService.updateOpportunity(dto.getId(), dto); return "Opportunity updated."; }
            else { saved = crmService.createOpportunity(dto); return "Opportunity " + saved.getOpportunityNo() + " created."; }
        });
    }

    @PostMapping("/crm/opportunities/stage/{id}")
    @ResponseBody
    public Map<String, Object> oppStage(@PathVariable Long id,
                                         @RequestParam String stage,
                                         @RequestParam(required = false) String lostReason) {
        return ok(() -> { crmService.updateStage(id, stage, lostReason); return "Stage updated to " + stage + "."; });
    }

    @DeleteMapping("/crm/opportunities/delete/{id}")
    @ResponseBody
    public Map<String, Object> oppDelete(@PathVariable Long id) {
        return ok(() -> { crmService.deleteOpportunity(id); return "Opportunity deleted."; });
    }

    @GetMapping("/crm/opportunities/pipeline")
    @ResponseBody
    public List<Map<String, Object>> oppPipeline() { return crmService.pipelineSummary(); }

    // ── CONTACTS ───────────────────────────────────────────────────────────────

    @GetMapping("/crm/contacts/list")
    @ResponseBody
    public DataTableResponse contactList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(required = false) Long customerId) {
        return crmService.contactDatatable(draw, start, length, search, customerId);
    }

    @GetMapping("/crm/contacts/show/{id}")
    @ResponseBody
    public Map<String, Object> contactShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", crmService.findContactById(id)));
    }

    @PostMapping("/crm/contacts/save")
    @ResponseBody
    public Map<String, Object> contactSave(@RequestBody @Valid ContactDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { crmService.updateContact(dto.getId(), dto); return "Contact updated."; }
            else { crmService.createContact(dto); return "Contact created."; }
        });
    }

    @PostMapping("/crm/contacts/toggle/{id}")
    @ResponseBody
    public Map<String, Object> contactToggle(@PathVariable Long id) {
        return ok(() -> { ContactDTO dto = crmService.toggleContact(id); return "Contact " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + "."; });
    }

    @DeleteMapping("/crm/contacts/delete/{id}")
    @ResponseBody
    public Map<String, Object> contactDelete(@PathVariable Long id) {
        return ok(() -> { crmService.deleteContact(id); return "Contact deleted."; });
    }

    // ── ACTIVITIES ─────────────────────────────────────────────────────────────

    @GetMapping("/crm/activities/list")
    @ResponseBody
    public DataTableResponse activityList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "")   String type,
            @RequestParam(defaultValue = "")   String status) {
        return crmService.activityDatatable(draw, start, length, search, type, status);
    }

    @GetMapping("/crm/activities/show/{id}")
    @ResponseBody
    public Map<String, Object> activityShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", crmService.findActivityById(id)));
    }

    @PostMapping("/crm/activities/save")
    @ResponseBody
    public Map<String, Object> activitySave(@RequestBody @Valid CrmActivityDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { crmService.updateActivity(dto.getId(), dto); return "Activity updated."; }
            else { crmService.createActivity(dto); return "Activity logged."; }
        });
    }

    @PostMapping("/crm/activities/complete/{id}")
    @ResponseBody
    public Map<String, Object> activityComplete(
            @PathVariable Long id,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String nextAction,
            @RequestParam(required = false) String nextActionDate) {
        return ok(() -> {
            LocalDate nad = (nextActionDate != null && !nextActionDate.isBlank()) ? LocalDate.parse(nextActionDate) : null;
            crmService.completeActivity(id, outcome, nextAction, nad);
            return "Activity marked as completed.";
        });
    }

    @DeleteMapping("/crm/activities/delete/{id}")
    @ResponseBody
    public Map<String, Object> activityDelete(@PathVariable Long id) {
        return ok(() -> { crmService.deleteActivity(id); return "Activity deleted."; });
    }

    // ── FEEDBACK ───────────────────────────────────────────────────────────────

    @GetMapping("/crm/feedback/list")
    @ResponseBody
    public DataTableResponse feedbackList(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search,
            @RequestParam(defaultValue = "")   String status,
            @RequestParam(defaultValue = "")   String type) {
        return crmService.feedbackDatatable(draw, start, length, search, status, type);
    }

    @GetMapping("/crm/feedback/show/{id}")
    @ResponseBody
    public Map<String, Object> feedbackShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", crmService.findFeedbackById(id)));
    }

    @PostMapping("/crm/feedback/save")
    @ResponseBody
    public Map<String, Object> feedbackSave(@RequestBody @Valid CustomerFeedbackDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { crmService.updateFeedback(dto.getId(), dto); return "Feedback updated."; }
            else { crmService.createFeedback(dto); return "Feedback recorded."; }
        });
    }

    @PostMapping("/crm/feedback/resolve/{id}")
    @ResponseBody
    public Map<String, Object> feedbackResolve(
            @PathVariable Long id,
            @RequestParam(required = false) String resolution) {
        return ok(() -> { crmService.resolveFeedback(id, resolution); return "Feedback resolved."; });
    }

    @DeleteMapping("/crm/feedback/delete/{id}")
    @ResponseBody
    public Map<String, Object> feedbackDelete(@PathVariable Long id) {
        return ok(() -> { crmService.deleteFeedback(id); return "Feedback deleted."; });
    }

    // ── Shared response builder ────────────────────────────────────────────────

    private Map<String, Object> ok(CheckedSupplier<Object> action) {
        Map<String, Object> res = new HashMap<>();
        try {
            Object result = action.get();
            res.put("success", true);
            if (result instanceof String msg) { res.put("message", msg); }
            else if (result instanceof Map<?,?> m) { res.put("obj", m); }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @FunctionalInterface
    interface CheckedSupplier<T> { T get() throws Exception; }
}
