package com.asg.spindleserp.crm.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.crm.dto.*;
import com.asg.spindleserp.crm.entity.*;

import java.util.List;
import java.util.Map;

/**
 * CrmService — unified interface for the CRM module.
 *
 * Covers:
 *   Lead management         (CRUD + status transitions)
 *   Opportunity pipeline    (CRUD + stage management)
 *   Contact management      (per-customer contacts)
 *   CRM Activities          (calls, emails, meetings)
 *   Customer Feedback       (complaints, ratings, resolution)
 *
 * All DataTable listings use JdbcTemplate ROW_NUMBER() OVER pattern.
 * AJAX Select2 search methods return {items:[{id,text}], hasMore}.
 */
public interface CrmService {

    // ── Leads ─────────────────────────────────────────────────────────────────

    LeadDTO createLead(LeadDTO dto);
    LeadDTO updateLead(Long id, LeadDTO dto);
    LeadDTO findLeadById(Long id);
    void    deleteLead(Long id);
    /** Change lead status: CONTACTED / QUALIFIED / UNQUALIFIED / CONVERTED / LOST / DORMANT */
    LeadDTO updateLeadStatus(Long id, String newStatus);
    DataTableResponse leadDatatable(int draw, int start, int length, String search, String status);
    Map<String, Object> searchLeads(String q, int page);
    LeadDTO toDTO(Lead entity);

    // ── Opportunities ─────────────────────────────────────────────────────────

    OpportunityDTO createOpportunity(OpportunityDTO dto);
    OpportunityDTO updateOpportunity(Long id, OpportunityDTO dto);
    OpportunityDTO findOpportunityById(Long id);
    void           deleteOpportunity(Long id);
    /** Advance or retract pipeline stage */
    OpportunityDTO updateStage(Long id, String stage, String lostReason);
    DataTableResponse opportunityDatatable(int draw, int start, int length, String search, String stage);
    /** Pipeline summary: count + value per stage */
    List<Map<String, Object>> pipelineSummary();
    OpportunityDTO toDTO(Opportunity entity);

    // ── Contacts ─────────────────────────────────────────────────────────────

    ContactDTO createContact(ContactDTO dto);
    ContactDTO updateContact(Long id, ContactDTO dto);
    ContactDTO findContactById(Long id);
    void       deleteContact(Long id);
    /** Toggle active status */
    ContactDTO toggleContact(Long id);
    DataTableResponse contactDatatable(int draw, int start, int length, String search, Long customerId);
    ContactDTO toDTO(Contact entity);

    // ── Activities ────────────────────────────────────────────────────────────

    CrmActivityDTO createActivity(CrmActivityDTO dto);
    CrmActivityDTO updateActivity(Long id, CrmActivityDTO dto);
    CrmActivityDTO findActivityById(Long id);
    void           deleteActivity(Long id);
    /** Mark activity as COMPLETED or CANCELLED */
    CrmActivityDTO completeActivity(Long id, String outcome, String nextAction, java.time.LocalDate nextActionDate);
    DataTableResponse activityDatatable(int draw, int start, int length, String search, String type, String status);
    CrmActivityDTO toDTO(CrmActivity entity);

    // ── Customer Feedback ─────────────────────────────────────────────────────

    CustomerFeedbackDTO createFeedback(CustomerFeedbackDTO dto);
    CustomerFeedbackDTO updateFeedback(Long id, CustomerFeedbackDTO dto);
    CustomerFeedbackDTO findFeedbackById(Long id);
    void                deleteFeedback(Long id);
    /** Resolve a feedback ticket */
    CustomerFeedbackDTO resolveFeedback(Long id, String resolution);
    DataTableResponse feedbackDatatable(int draw, int start, int length, String search, String status, String type);
    CustomerFeedbackDTO toDTO(CustomerFeedback entity);

    // ── Dashboard summary ─────────────────────────────────────────────────────

    /** Returns counts for dashboard KPI cards */
    Map<String, Object> dashboardSummary();
}
