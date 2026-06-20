package com.asg.spindleserp.approval.service;

import com.asg.spindleserp.approval.dto.*;
import com.asg.spindleserp.approval.entity.*;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

/**
 * ApprovalService — unified service for the Approval module.
 *
 * Covers:
 *   Approval Configs   — define workflow templates with levels and approvers
 *   Approval Requests  — runtime instances of a workflow
 *   Approval Inbox     — pending approvals for the logged-in user
 *   Approval Actions   — approve / reject / return / hold on a request
 *   Approval Delegation — delegate approval authority to another user
 *
 * Flow (SEQUENTIAL):
 *   DRAFT → SUBMITTED → IN_APPROVAL (level 1) → … → IN_APPROVAL (level N) → APPROVED
 *                                                  → REJECTED (at any level)
 *                                                  → RETURNED (back to requester)
 *                                                  → HOLD     (paused)
 *
 * Flow (PARALLEL):
 *   All levels receive the request simultaneously; majority or all must approve.
 */
public interface ApprovalService {

    // ── Config ────────────────────────────────────────────────────────────────

    ApprovalConfigDTO createConfig(ApprovalConfigDTO dto);
    ApprovalConfigDTO updateConfig(Long id, ApprovalConfigDTO dto);
    ApprovalConfigDTO findConfigById(Long id);
    void              deleteConfig(Long id);
    ApprovalConfigDTO toggleConfig(Long id);
    DataTableResponse configDatatable(int draw, int start, int length, String search);
    Map<String, Object> searchConfigs(String q, int page);
    ApprovalConfigDTO toDTO(ApprovalConfig entity);

    // ── Request — lifecycle ───────────────────────────────────────────────────

    /**
     * Submit a new approval request for a document.
     * Looks up the matching ApprovalConfig by documentType + orgId,
     * creates the ApprovalRequest, sets level 1 approver, sends notification.
     */
    ApprovalRequestDTO submitRequest(ApprovalRequestDTO dto);

    ApprovalRequestDTO findRequestById(Long id);
    ApprovalRequestDTO findRequestByReference(Long refId, String docType);
    void               cancelRequest(Long id, String reason);

    DataTableResponse requestDatatable(int draw, int start, int length, String search, String status);

    // ── Inbox — my pending approvals ──────────────────────────────────────────

    DataTableResponse inboxDatatable(int draw, int start, int length, String search);

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Approve current level; advance to next level or mark APPROVED if last */
    ApprovalRequestDTO approve(Long requestId, String comments);

    /** Reject — terminate workflow at current level → status = REJECTED */
    ApprovalRequestDTO reject(Long requestId, String reason);

    /** Return to requester for correction → status = RETURNED */
    ApprovalRequestDTO returnForCorrection(Long requestId, String reason);

    /** Hold/pause the request */
    ApprovalRequestDTO hold(Long requestId, String comments);

    /** Release from hold back to IN_APPROVAL */
    ApprovalRequestDTO release(Long requestId);

    // ── History ───────────────────────────────────────────────────────────────

    List<ApprovalRequestDTO.HistoryDTO> getHistory(Long requestId);

    // ── Delegation ────────────────────────────────────────────────────────────

    ApprovalDelegationDTO createDelegation(ApprovalDelegationDTO dto);
    ApprovalDelegationDTO revokeDelegation(Long id, String reason);
    ApprovalDelegationDTO findDelegationById(Long id);
    DataTableResponse delegationDatatable(int draw, int start, int length, String search);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    Map<String, Object> dashboardSummary();

    // ── Mapping helpers ───────────────────────────────────────────────────────

    ApprovalRequestDTO toDTO(ApprovalRequest entity);
    ApprovalDelegationDTO toDTO(ApprovalDelegation entity);
}
