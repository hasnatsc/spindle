package com.asg.spindleserp.approval.service;

import com.asg.spindleserp.approval.dto.*;
import com.asg.spindleserp.approval.entity.*;
import com.asg.spindleserp.approval.repository.*;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalConfigRepository     configRepo;
    private final ApprovalLevelRepository      levelRepo;
    private final ApprovalRequestRepository    requestRepo;
    private final ApprovalHistoryRepository    historyRepo;
    private final ApprovalDelegationRepository delegationRepo;
    private final UserRepository               userRepo;
    private final OrganizationRepository       orgRepo;
    private final JdbcTemplate                 jdbcTemplate;

    // =========================================================================
    // CONFIG
    // =========================================================================

    @Override
    public ApprovalConfigDTO createConfig(ApprovalConfigDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (configRepo.findByCode(dto.getCode().trim().toUpperCase()).isPresent())
            throw new IllegalArgumentException("Config code '" + dto.getCode() + "' already exists.");
        ApprovalConfig config = buildConfig(dto, new ApprovalConfig());
        config.setOrganization(orgRepo.getReferenceById(orgId));
        ApprovalConfig saved = configRepo.save(config);
        syncLevels(dto.getLevels(), saved);
        return toDTO(saved);
    }

    @Override
    public ApprovalConfigDTO updateConfig(Long id, ApprovalConfigDTO dto) {
        ApprovalConfig config = findConfig(id);
        buildConfig(dto, config);
        ApprovalConfig saved = configRepo.save(config);
        syncLevels(dto.getLevels(), saved);
        return toDTO(saved);
    }

    @Override @Transactional(readOnly = true)
    public ApprovalConfigDTO findConfigById(Long id) { return toDTO(findConfig(id)); }

    @Override
    public void deleteConfig(Long id) {
        ApprovalConfig c = findConfig(id);
        if (requestRepo.findByCurrentApproverUserIdAndStatus(null, "IN_APPROVAL").stream()
                .anyMatch(r -> r.getApprovalConfig() != null && r.getApprovalConfig().getId().equals(id)))
            throw new IllegalStateException("Config has active approval requests.");
        configRepo.delete(c);
    }

    @Override
    public ApprovalConfigDTO toggleConfig(Long id) {
        ApprovalConfig c = findConfig(id);
        c.setActive(!c.isActive());
        return toDTO(configRepo.save(c));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse configDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND c.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("c.code","c.name","c.document_type","c.module"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY c.id DESC) AS sl,
                   COUNT(*) OVER ()                        AS full_count,
                   c.id, c.code, c.name, c.document_type, c.module, c.flow_type,
                   c.priority, c.auto_escalation_hours,
                   COALESCE(c.min_amount::text,'—') AS min_amount,
                   COALESCE(c.max_amount::text,'—') AS max_amount,
                   (SELECT COUNT(*) FROM apr_levels l WHERE l.approval_config_id = c.id) AS level_count,
                   CASE WHEN c.is_active
                       THEN '<span class="badge bg-success">Active</span>'
                       ELSE '<span class="badge bg-secondary">Inactive</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="cfgShow('   || c.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="cfgEdit('   || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="cfgToggle(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="cfgDelete(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM apr_configs c
            %s ORDER BY c.priority ASC NULLS LAST, c.code OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> searchConfigs(String q, int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page-1)*sz;
        String sql = "SELECT id, code, name FROM apr_configs WHERE is_active=true"
            + (orgId != null ? " AND organization_id=" + orgId : "")
            + (q != null && !q.isBlank() ? " AND (code ILIKE '%" + q.replace("'","''") + "%' OR name ILIKE '%" + q.replace("'","''") + "%')" : "")
            + " ORDER BY priority ASC NULLS LAST, code LIMIT " + (sz+1) + " OFFSET " + off;
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        boolean more = rows.size() > sz;
        List<Map<String,Object>> items = rows.stream().limit(sz).map(r ->
            Map.of("id", r.get("id"), "text", r.get("code") + " — " + r.get("name"))).toList();
        return Map.of("items", items, "hasMore", more);
    }

    @Override
    public ApprovalConfigDTO toDTO(ApprovalConfig e) {
        ApprovalConfigDTO d = ApprovalConfigDTO.builder()
            .id(e.getId()).code(e.getCode()).name(e.getName()).description(e.getDescription())
            .documentType(e.getDocumentType()).module(e.getModule()).flowType(e.getFlowType())
            .active(e.isActive()).enableReminders(e.isEnableReminders())
            .useReportingHierarchy(e.isUseReportingHierarchy())
            .priority(e.getPriority()).autoEscalationHours(e.getAutoEscalationHours())
            .reminderIntervalHours(e.getReminderIntervalHours())
            .minAmount(e.getMinAmount()).maxAmount(e.getMaxAmount())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        List<ApprovalLevel> lvls = levelRepo.findByApprovalConfigIdOrderByLevelNumberAsc(e.getId());
        d.setLevels(lvls.stream().map(l -> {
            ApprovalConfigDTO.LevelDTO ld = ApprovalConfigDTO.LevelDTO.builder()
                .id(l.getId()).levelNumber(l.getLevelNumber()).levelName(l.getLevelName())
                .description(l.getDescription()).approverDescription(l.getApproverDescription())
                .active(l.isActive()).canApproveWithChanges(l.isCanApproveWithChanges())
                .canDelegate(l.isCanDelegate()).canForward(l.isCanForward()).canHold(l.isCanHold())
                .build();
            if (l.getApproverUser() != null) {
                ld.setApproverUserId(l.getApproverUser().getId());
                ld.setApproverUserDisplay(displayName(l.getApproverUser().getFullName(), l.getApproverUser().getUsername()));
            }
            return ld;
        }).collect(Collectors.toList()));
        return d;
    }

    // =========================================================================
    // REQUEST
    // =========================================================================

    @Override
    public ApprovalRequestDTO submitRequest(ApprovalRequestDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        Long userId = ContextProvider.getCurrentUserId();

        // Find config for this document type
        ApprovalConfig config = configRepo
            .findByOrganizationIdAndDocumentTypeAndIsActiveTrue(orgId, dto.getDocumentType())
            .stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No active approval config found for document type: " + dto.getDocumentType()));

        List<ApprovalLevel> levels = levelRepo.findByApprovalConfigIdOrderByLevelNumberAsc(config.getId());
        if (levels.isEmpty())
            throw new IllegalStateException("Approval config '" + config.getCode() + "' has no levels.");

        ApprovalLevel firstLevel = levels.getFirst();

        ApprovalRequest req = ApprovalRequest.builder()
            .approvalConfig(config)
            .requester(userRepo.getReferenceById(userId))
            .requesterName(ContextProvider.getCurrentUsername())
            .documentType(dto.getDocumentType())
            .referenceId(dto.getReferenceId())
            .referenceNumber(dto.getReferenceNumber())
            .documentDate(dto.getDocumentDate())
            .documentAmount(dto.getDocumentAmount())
            .documentSummary(dto.getDocumentSummary())
            .currentLevelNumber(1)
            .totalLevels(levels.size())
            .currentApprovalLevel(firstLevel)
            .currentApproverUser(firstLevel.getApproverUser())
            .currentApproverRole(firstLevel.getLevelName())
            .status("IN_APPROVAL")
            .isUrgent(Boolean.TRUE.equals(dto.getIsUrgent()))
            .dueDate(dto.getDueDate())
            .build();
        req.setOrganization(orgRepo.getReferenceById(orgId));
        setAudit(req, true);
        ApprovalRequest saved = requestRepo.save(req);

        // Record history entry for submission
        recordHistory(saved, firstLevel, userId, "SUBMITTED", "Submitted for approval", null, null);

        return toDTO(saved);
    }

    @Override @Transactional(readOnly = true)
    public ApprovalRequestDTO findRequestById(Long id) { return toDTO(findRequest(id)); }

    @Override @Transactional(readOnly = true)
    public ApprovalRequestDTO findRequestByReference(Long refId, String docType) {
        return requestRepo.findByReferenceIdAndDocumentType(refId, docType)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("No request for " + docType + " #" + refId));
    }

    @Override
    public void cancelRequest(Long id, String reason) {
        ApprovalRequest req = findRequest(id);
        guardMutable(req);
        req.setStatus("CANCELLED");
        req.setFinalRemarks(reason);
        req.setFinalActionBy(ContextProvider.getCurrentUsername());
        req.setCompletedAt(LocalDateTime.now());
        setAudit(req, false);
        requestRepo.save(req);
        recordHistory(req, req.getCurrentApprovalLevel(), ContextProvider.getCurrentUserId(), "CANCELLED", reason, null, null);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse requestDatatable(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1"
            + (orgId != null ? " AND r.organization_id = " + orgId : "")
            + (status != null && !status.isBlank() ? " AND r.status = '" + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList("r.reference_number","r.document_type","r.requester_name","r.status"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY r.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   r.id, r.document_type, r.reference_number, r.requester_name, r.status,
                   TO_CHAR(r.document_date,'DD-Mon-YYYY') AS document_date,
                   COALESCE(r.document_amount::text,'—') AS document_amount,
                   r.current_level_number, r.total_levels,
                   COALESCE(r.document_summary,'—') AS document_summary,
                   COALESCE(r.current_approver_role,'—') AS current_approver_role,
                   COALESCE(u.full_name,u.username,'—') AS current_approver,
                   TO_CHAR(r.created_at,'DD-Mon-YYYY') AS submitted_at,
                   COALESCE(TO_CHAR(r.due_date,'DD-Mon-YYYY'),'—') AS due_date,
                   r.is_urgent,
                   CASE r.status
                       WHEN 'DRAFT'       THEN '<span class="badge bg-secondary">Draft</span>'
                       WHEN 'SUBMITTED'   THEN '<span class="badge bg-info text-dark">Submitted</span>'
                       WHEN 'IN_APPROVAL' THEN '<span class="badge bg-primary">In Approval</span>'
                       WHEN 'APPROVED'    THEN '<span class="badge bg-success">Approved</span>'
                       WHEN 'REJECTED'    THEN '<span class="badge bg-danger">Rejected</span>'
                       WHEN 'RETURNED'    THEN '<span class="badge bg-warning text-dark">Returned</span>'
                       WHEN 'CANCELLED'   THEN '<span class="badge bg-secondary">Cancelled</span>'
                       WHEN 'HOLD'        THEN '<span class="badge bg-orange">On Hold</span>'
                       WHEN 'COMPLETED'   THEN '<span class="badge bg-teal">Completed</span>'
                       ELSE '<span class="badge bg-light text-dark">' || r.status || '</span>'
                   END AS status_badge,
                   CASE WHEN r.is_urgent THEN '<span class="badge bg-danger">URGENT</span>' ELSE '' END AS urgent_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="reqShow('    || r.id || ')" class="btn btn-white btn-sm" title="View History"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN r.status IN (''IN_APPROVAL'',''SUBMITTED'') THEN
                       '<a href="javascript:;" onclick="reqCancel(' || r.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-secondary"></i></a>'
                      ELSE '' END
                   || '</div>' AS actions
            FROM apr_requests r
            LEFT JOIN users u ON u.id = r.current_approver_user_id
            %s ORDER BY r.is_urgent DESC, r.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // INBOX
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public DataTableResponse inboxDatatable(int draw, int start, int length, String search) {
        Long orgId  = SecurityHelper.currentOrgId().orElse(null);
        Long userId = ContextProvider.getCurrentUserId();
        String where = "WHERE r.status IN ('IN_APPROVAL','SUBMITTED') AND r.current_approver_user_id = " + userId
            + (orgId != null ? " AND r.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList("r.reference_number","r.document_type","r.requester_name"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY r.is_urgent DESC, r.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   r.id, r.document_type, r.reference_number, r.requester_name,
                   COALESCE(r.document_amount::text,'—') AS document_amount,
                   TO_CHAR(r.document_date,'DD-Mon-YYYY') AS document_date,
                   r.current_level_number, r.total_levels,
                   COALESCE(r.document_summary,'—') AS document_summary,
                   COALESCE(TO_CHAR(r.due_date,'DD-Mon-YYYY'),'—') AS due_date,
                   r.is_urgent,
                   CASE WHEN r.is_urgent THEN '<span class="badge bg-danger">URGENT</span>' ELSE '<span class="badge bg-info text-dark">Normal</span>' END AS urgent_badge,
                   'Level ' || r.current_level_number || ' of ' || r.total_levels AS progress,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="inboxShow('    || r.id || ')" class="btn btn-white btn-sm" title="Review"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="inboxApprove(' || r.id || ')" class="btn btn-white btn-sm" title="Approve"><i class="fas fa-check-circle text-success"></i></a>'
                   || '<a href="javascript:;" onclick="inboxReject('  || r.id || ')" class="btn btn-white btn-sm" title="Reject"><i class="fas fa-times-circle text-danger"></i></a>'
                   || '<a href="javascript:;" onclick="inboxReturn('  || r.id || ')" class="btn btn-white btn-sm" title="Return"><i class="fas fa-reply text-warning"></i></a>'
                   || '</div>' AS actions
            FROM apr_requests r
            %s ORDER BY r.is_urgent DESC, r.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    @Override
    public ApprovalRequestDTO approve(Long requestId, String comments) {
        ApprovalRequest req = findRequest(requestId);
        guardActionAllowed(req);
        Long userId = ContextProvider.getCurrentUserId();
        ApprovalLevel currentLevel = req.getCurrentApprovalLevel();
        recordHistory(req, currentLevel, userId, "APPROVED", comments, null, null);

        // Check if more levels remain (SEQUENTIAL)
        List<ApprovalLevel> allLevels = levelRepo.findByApprovalConfigIdOrderByLevelNumberAsc(
            req.getApprovalConfig().getId());
        int nextLevelNum = req.getCurrentLevelNumber() + 1;

        Optional<ApprovalLevel> nextLevel = allLevels.stream()
            .filter(l -> l.getLevelNumber() == nextLevelNum && l.isActive())
            .findFirst();

        if (nextLevel.isPresent()) {
            // Advance to next level
            req.setCurrentApprovalLevel(nextLevel.get());
            req.setCurrentApproverUser(nextLevel.get().getApproverUser());
            req.setCurrentApproverRole(nextLevel.get().getLevelName());
            req.setCurrentLevelNumber(nextLevelNum);
            req.setStatus("IN_APPROVAL");
        } else {
            // All levels done → APPROVED
            req.setStatus("APPROVED");
            req.setFinalActionBy(ContextProvider.getCurrentUsername());
            req.setFinalRemarks(comments);
            req.setCompletedAt(LocalDateTime.now());
        }
        setAudit(req, false);
        return toDTO(requestRepo.save(req));
    }

    @Override
    public ApprovalRequestDTO reject(Long requestId, String reason) {
        ApprovalRequest req = findRequest(requestId);
        guardActionAllowed(req);
        Long userId = ContextProvider.getCurrentUserId();
        recordHistory(req, req.getCurrentApprovalLevel(), userId, "REJECTED", null, reason, null);
        req.setStatus("REJECTED");
        req.setFinalActionBy(ContextProvider.getCurrentUsername());
        req.setFinalRemarks(reason);
        req.setCompletedAt(LocalDateTime.now());
        setAudit(req, false);
        return toDTO(requestRepo.save(req));
    }

    @Override
    public ApprovalRequestDTO returnForCorrection(Long requestId, String reason) {
        ApprovalRequest req = findRequest(requestId);
        guardActionAllowed(req);
        Long userId = ContextProvider.getCurrentUserId();
        recordHistory(req, req.getCurrentApprovalLevel(), userId, "RETURNED", null, null, reason);
        req.setStatus("RETURNED");
        req.setFinalRemarks(reason);
        setAudit(req, false);
        return toDTO(requestRepo.save(req));
    }

    @Override
    public ApprovalRequestDTO hold(Long requestId, String comments) {
        ApprovalRequest req = findRequest(requestId);
        guardActionAllowed(req);
        recordHistory(req, req.getCurrentApprovalLevel(), ContextProvider.getCurrentUserId(), "HOLD", comments, null, null);
        req.setStatus("HOLD");
        setAudit(req, false);
        return toDTO(requestRepo.save(req));
    }

    @Override
    public ApprovalRequestDTO release(Long requestId) {
        ApprovalRequest req = findRequest(requestId);
        if (!"HOLD".equals(req.getStatus()))
            throw new IllegalStateException("Request is not on hold.");
        recordHistory(req, req.getCurrentApprovalLevel(), ContextProvider.getCurrentUserId(), "RELEASED", "Released from hold", null, null);
        req.setStatus("IN_APPROVAL");
        setAudit(req, false);
        return toDTO(requestRepo.save(req));
    }

    // =========================================================================
    // HISTORY
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public List<ApprovalRequestDTO.HistoryDTO> getHistory(Long requestId) {
        return historyRepo.findByApprovalRequestIdOrderByActionAtDesc(requestId).stream().map(h ->
            ApprovalRequestDTO.HistoryDTO.builder()
                .id(h.getId()).levelNumber(h.getLevelNumber()).levelName(h.getLevelName())
                .actorName(h.getActorName()).actorDesignation(h.getActorDesignation())
                .action(h.getAction()).status(h.getStatus()).comments(h.getComments())
                .rejectionReason(h.getRejectionReason()).returnReason(h.getReturnReason())
                .isAutoAction(h.isAutoAction()).actionAt(h.getActionAt())
                .build()).collect(Collectors.toList());
    }

    // =========================================================================
    // DELEGATION
    // =========================================================================

    @Override
    public ApprovalDelegationDTO createDelegation(ApprovalDelegationDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        Long userId = ContextProvider.getCurrentUserId();
        String code = "DEL-" + System.currentTimeMillis();
        ApprovalDelegation del = ApprovalDelegation.builder()
            .delegator(userRepo.getReferenceById(userId))
            .delegate(userRepo.getReferenceById(dto.getDelegateId()))
            .delegationCode(code)
            .module(dto.getModule())
            .documentType(dto.getDocumentType())
            .startDate(dto.getStartDate())
            .endDate(dto.getEndDate())
            .maxAmount(dto.getMaxAmount())
            .reason(dto.getReason())
            .status("SCHEDULED")
            .isActive(true)
            .notifyDelegator(Boolean.TRUE.equals(dto.getNotifyDelegator()))
            .build();
        del.setOrganization(orgRepo.getReferenceById(orgId));
        setAudit(del, true);
        return toDTO(delegationRepo.save(del));
    }

    @Override
    public ApprovalDelegationDTO revokeDelegation(Long id, String reason) {
        ApprovalDelegation del = findDelegation(id);
        del.setStatus("REVOKED");
        del.setActive(false);
        del.setRevocationReason(reason);
        del.setRevokedAt(LocalDateTime.now());
        del.setRevokedBy(userRepo.getReferenceById(ContextProvider.getCurrentUserId()));
        return toDTO(delegationRepo.save(del));
    }

    @Override @Transactional(readOnly = true)
    public ApprovalDelegationDTO findDelegationById(Long id) { return toDTO(findDelegation(id)); }

    @Override @Transactional(readOnly = true)
    public DataTableResponse delegationDatatable(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String where = "WHERE 1=1" + (orgId != null ? " AND d.organization_id = " + orgId : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "d.delegation_code","d.module","d.document_type","d.status"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY d.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   d.id, d.delegation_code, d.module, d.document_type, d.status,
                   COALESCE(du.full_name,du.username,'—') AS delegator_name,
                   COALESCE(ee.full_name,ee.username,'—') AS delegate_name,
                   TO_CHAR(d.start_date,'DD-Mon-YYYY') AS start_date,
                   TO_CHAR(d.end_date,'DD-Mon-YYYY')   AS end_date,
                   COALESCE(d.max_amount::text,'No limit') AS max_amount,
                   COALESCE(d.reason,'—') AS reason,
                   CASE d.status
                       WHEN 'SCHEDULED' THEN '<span class="badge bg-info text-dark">Scheduled</span>'
                       WHEN 'ACTIVE'    THEN '<span class="badge bg-success">Active</span>'
                       WHEN 'EXPIRED'   THEN '<span class="badge bg-secondary">Expired</span>'
                       WHEN 'REVOKED'   THEN '<span class="badge bg-danger">Revoked</span>'
                       ELSE '<span class="badge bg-light text-dark">' || d.status || '</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="delShow('   || d.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                   || CASE WHEN d.status IN (''SCHEDULED'','ACTIVE') THEN
                       '<a href="javascript:;" onclick="delRevoke(' || d.id || ')" class="btn btn-white btn-sm" title="Revoke"><i class="fas fa-ban text-danger"></i></a>'
                      ELSE '' END
                   || '</div>' AS actions
            FROM apr_delegations d
            JOIN users du ON du.id = d.delegator_id
            JOIN users ee ON ee.id = d.delegate_id
            %s ORDER BY d.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long   orgId  = SecurityHelper.currentOrgId().orElse(null);
        Long   userId = ContextProvider.getCurrentUserId();
        String f      = orgId != null ? " AND r.organization_id = " + orgId : "";
        String fNR    = orgId != null ? " AND organization_id = " + orgId : "";
        String today  = java.time.LocalDate.now().toString();
        String mtdStart = java.time.LocalDate.now().withDayOfMonth(1).toString();

        Map<String, Object> m = new LinkedHashMap<>();

        // ── 1. Status counts + MTD + totals — single pass ──────────────────────
        String kpiSql = """
        SELECT
          COUNT(*)                                                AS total_requests,
          COUNT(*) FILTER (WHERE status IN ('IN_APPROVAL','SUBMITTED')
                             AND current_approver_user_id = """ + userId + """
                     )                                           AS pending_inbox,
          COUNT(*) FILTER (WHERE status = 'IN_APPROVAL')         AS total_in_approval,
          COUNT(*) FILTER (WHERE status = 'APPROVED')            AS total_approved,
          COUNT(*) FILTER (WHERE status = 'REJECTED')            AS total_rejected,
          COUNT(*) FILTER (WHERE status = 'RETURNED')            AS total_returned,
          COUNT(*) FILTER (WHERE status = 'HOLD')                AS total_on_hold,
          COUNT(*) FILTER (WHERE status = 'CANCELLED')           AS total_cancelled,
          COUNT(*) FILTER (WHERE status IN ('IN_APPROVAL','SUBMITTED')
                             AND is_urgent = true)               AS urgent_pending,
          COUNT(*) FILTER (WHERE status = 'APPROVED'
                             AND completed_at >= ?::timestamp)    AS approved_mtd,
          COUNT(*) FILTER (WHERE status = 'REJECTED'
                             AND completed_at >= ?::timestamp)    AS rejected_mtd,
          COUNT(*) FILTER (WHERE status IN ('IN_APPROVAL','SUBMITTED')
                             AND due_date IS NOT NULL
                             AND due_date < ?::date)             AS overdue_count,
          COUNT(*) FILTER (WHERE status IN ('IN_APPROVAL','SUBMITTED')
                             AND due_date IS NOT NULL
                             AND due_date < (CURRENT_DATE - INTERVAL '2 days'))
                                                                  AS sla_breach_count
        FROM apr_requests r WHERE 1=1""" + f;

        List<Map<String, Object>> kpiRows = jdbcTemplate.queryForList(kpiSql, mtdStart, mtdStart, today);
        if (!kpiRows.isEmpty()) {
            Map<String, Object> r = kpiRows.get(0);
            m.put("totalRequests",   toLong(r, "total_requests"));
            m.put("pendingInbox",    toLong(r, "pending_inbox"));
            m.put("totalInApproval", toLong(r, "total_in_approval"));
            m.put("totalApproved",   toLong(r, "total_approved"));
            m.put("totalRejected",   toLong(r, "total_rejected"));
            m.put("totalReturned",   toLong(r, "total_returned"));
            m.put("totalOnHold",     toLong(r, "total_on_hold"));
            m.put("totalCancelled",  toLong(r, "total_cancelled"));
            m.put("urgentPending",   toLong(r, "urgent_pending"));
            m.put("approvedMTD",     toLong(r, "approved_mtd"));
            m.put("rejectedMTD",     toLong(r, "rejected_mtd"));
            m.put("overdueCount",    toLong(r, "overdue_count"));
            m.put("slaBreachCount",  toLong(r, "sla_breach_count"));
        }

        // ── 2. Avg approval hours (completed requests) ──────────────────────────
        String avgSql = """
        SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - created_at))/3600.0), 0) AS avg_hours
        FROM apr_requests r WHERE status = 'APPROVED' AND completed_at IS NOT NULL
        """ + f;
        List<Map<String, Object>> avgRows = jdbcTemplate.queryForList(avgSql);
        if (!avgRows.isEmpty()) {
            Object v = avgRows.get(0).get("avg_hours");
            double avg = (v != null && v instanceof Number n) ? Math.round(n.doubleValue() * 10.0) / 10.0 : 0.0;
            m.put("avgApprovalHours", avg);
        }

        // ── 3. Delegations ──────────────────────────────────────────────────────
        String delSql = """
        SELECT COUNT(*) FILTER (WHERE status = 'ACTIVE' AND end_date >= ?::date) AS active_del,
               COUNT(*) FILTER (WHERE status = 'EXPIRED' OR end_date < ?::date)  AS expired_del
        FROM apr_delegations WHERE 1=1""" + fNR;
        List<Map<String, Object>> delRows = jdbcTemplate.queryForList(delSql, today, today);
        if (!delRows.isEmpty()) {
            m.put("activeDelegations",  toLong(delRows.get(0), "active_del"));
            m.put("expiredDelegations", toLong(delRows.get(0), "expired_del"));
        }

        // ── 4. Configs ──────────────────────────────────────────────────────────
        String cfgSql = "SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE is_active=true) AS active FROM apr_configs WHERE 1=1" + fNR;
        List<Map<String, Object>> cfgRows = jdbcTemplate.queryForList(cfgSql);
        if (!cfgRows.isEmpty()) {
            m.put("totalConfigs",  toLong(cfgRows.get(0), "total"));
            m.put("activeConfigs", toLong(cfgRows.get(0), "active"));
        }

        // ── 5. By status ────────────────────────────────────────────────────────
        m.put("byStatus", jdbcTemplate.queryForList("""
        SELECT status, COUNT(*) AS count
        FROM apr_requests r WHERE 1=1""" + f + """
        GROUP BY status ORDER BY count DESC"""));

        // ── 6. By document type — with approval/rejection split ─────────────────
        m.put("byDocType", jdbcTemplate.queryForList("""
        SELECT document_type,
               COUNT(*) AS count,
               COUNT(*) FILTER (WHERE status='APPROVED') AS approved,
               COUNT(*) FILTER (WHERE status='REJECTED') AS rejected,
               COUNT(*) FILTER (WHERE status IN ('IN_APPROVAL','SUBMITTED')) AS in_approval
        FROM apr_requests r WHERE 1=1""" + f + """
        GROUP BY document_type ORDER BY count DESC LIMIT 12"""));

        // ── 7. By module (from config) ───────────────────────────────────────────
        m.put("byModule", jdbcTemplate.queryForList("""
        SELECT COALESCE(c.module,'—') AS module,
               COUNT(r.id) AS count,
               COUNT(r.id) FILTER (WHERE r.status IN ('IN_APPROVAL','SUBMITTED')) AS in_approval,
               COUNT(r.id) FILTER (WHERE r.status='APPROVED') AS approved
        FROM apr_requests r
        LEFT JOIN apr_configs c ON c.id = r.approval_config_id
        WHERE 1=1""" + f + """
        GROUP BY c.module ORDER BY count DESC LIMIT 10"""));

        // ── 8. Top approvers by throughput ───────────────────────────────────────
        m.put("topApprovers", jdbcTemplate.queryForList("""
        SELECT COALESCE(u.full_name, u.username, '—') AS approver_name,
               COUNT(*) AS approved_count,
               COALESCE(AVG(EXTRACT(EPOCH FROM (r.completed_at - r.created_at))/3600.0), 0) AS avg_hours
        FROM apr_histories h
        JOIN apr_requests r ON r.id = h.approval_request_id
        JOIN sec_users u ON u.id = h.actor_user_id
        WHERE h.action = 'APPROVED'
          AND r.completed_at IS NOT NULL
        """ + (orgId != null ? " AND r.organization_id = " + orgId : "") + """
        GROUP BY u.id, u.full_name, u.username
        ORDER BY approved_count DESC
        LIMIT 8"""));

        // ── 9. Overdue requests ──────────────────────────────────────────────────
        m.put("overdueRequests", jdbcTemplate.queryForList("""
        SELECT r.id, r.reference_number, r.document_type, r.requester_name,
               TO_CHAR(r.due_date,'DD-Mon-YYYY') AS due_date,
               (CURRENT_DATE - r.due_date) AS days_overdue,
               r.is_urgent, r.current_level_number, r.total_levels,
               COALESCE(r.document_summary,'—') AS document_summary,
               COALESCE(u.full_name, u.username,'—') AS current_approver
        FROM apr_requests r
        LEFT JOIN sec_users u ON u.id = r.current_approver_user_id
        WHERE r.status IN ('IN_APPROVAL','SUBMITTED')
          AND r.due_date IS NOT NULL
          AND r.due_date < ?::date
        """ + (orgId != null ? " AND r.organization_id = " + orgId : "") + """
        ORDER BY days_overdue DESC
        LIMIT 15""", today));

        // ── 10. Urgent in-approval ───────────────────────────────────────────────
        m.put("urgentItems", jdbcTemplate.queryForList("""
        SELECT r.id, r.reference_number, r.document_type, r.requester_name,
               TO_CHAR(r.document_date,'DD-Mon-YYYY') AS document_date,
               COALESCE(TO_CHAR(r.due_date,'DD-Mon-YYYY'),'—') AS due_date,
               r.current_level_number, r.total_levels,
               COALESCE(r.document_amount,0) AS document_amount,
               COALESCE(r.document_summary,'—') AS document_summary,
               COALESCE(u.full_name, u.username,'—') AS current_approver
        FROM apr_requests r
        LEFT JOIN sec_users u ON u.id = r.current_approver_user_id
        WHERE r.status IN ('IN_APPROVAL','SUBMITTED')
          AND r.is_urgent = true
        """ + (orgId != null ? " AND r.organization_id = " + orgId : "") + """
        ORDER BY r.id DESC
        LIMIT 10"""));

        // ── 11. My inbox (current user) ──────────────────────────────────────────
        m.put("recentInbox", jdbcTemplate.queryForList("""
        SELECT r.id, r.reference_number, r.document_type, r.requester_name,
               TO_CHAR(r.document_date,'DD-Mon-YYYY') AS document_date,
               COALESCE(TO_CHAR(r.due_date,'DD-Mon-YYYY'),'—') AS due_date,
               COALESCE(r.document_amount,0) AS document_amount,
               r.is_urgent, r.status,
               r.current_level_number, r.total_levels,
               COALESCE(r.document_summary,'—') AS document_summary
        FROM apr_requests r
        WHERE r.status IN ('IN_APPROVAL','SUBMITTED')
          AND r.current_approver_user_id = """ + userId + (orgId != null ? " AND r.organization_id = " + orgId : "") + """
        ORDER BY r.is_urgent DESC, r.id DESC
        LIMIT 10"""));

        // ── 12. Recent history across org ────────────────────────────────────────
        m.put("recentActivity", jdbcTemplate.queryForList("""
        SELECT h.id, h.action, h.level_name, h.actor_name,
               r.reference_number, r.document_type,
               TO_CHAR(h.action_at,'DD-Mon-YYYY HH24:MI') AS action_at,
               COALESCE(h.comments,'') AS comments,
               COALESCE(h.rejection_reason,'') AS rejection_reason
        FROM apr_histories h
        JOIN apr_requests r ON r.id = h.approval_request_id
        WHERE 1=1""" + (orgId != null ? " AND r.organization_id = " + orgId : "") + """
        ORDER BY h.id DESC
        LIMIT 15"""));

        // ── 13. 12-month trend ───────────────────────────────────────────────────
        m.put("monthlyTrend", jdbcTemplate.queryForList("""
        SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'Mon-YY') AS month,
               DATE_TRUNC('month', created_at)                    AS month_order,
               COUNT(*) AS submitted,
               COUNT(*) FILTER (WHERE status = 'APPROVED') AS approved,
               COUNT(*) FILTER (WHERE status = 'REJECTED') AS rejected
        FROM apr_requests r WHERE 1=1""" + f + """
          AND created_at >= (CURRENT_DATE - INTERVAL '12 months')
        GROUP BY DATE_TRUNC('month', created_at)
        ORDER BY month_order"""));

        return m;
    }

// ── Helpers — add alongside existing private helpers in ApprovalServiceImpl ──

    private Long toLong(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    // =========================================================================
    // MAPPING
    // =========================================================================

    @Override
    public ApprovalRequestDTO toDTO(ApprovalRequest e) {
        ApprovalRequestDTO d = ApprovalRequestDTO.builder()
            .id(e.getId())
            .documentType(e.getDocumentType()).referenceId(e.getReferenceId())
            .referenceNumber(e.getReferenceNumber()).documentDate(e.getDocumentDate())
            .documentAmount(e.getDocumentAmount()).documentSummary(e.getDocumentSummary())
            .requesterId(e.getRequester() != null ? e.getRequester().getId() : null)
            .requesterName(e.getRequesterName())
            .currentLevelNumber(e.getCurrentLevelNumber()).totalLevels(e.getTotalLevels())
            .currentApproverRole(e.getCurrentApproverRole())
            .status(e.getStatus())
            .isUrgent(e.isUrgent()).dueDate(e.getDueDate())
            .finalRemarks(e.getFinalRemarks()).finalActionBy(e.getFinalActionBy())
            .completedAt(e.getCompletedAt())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getApprovalConfig() != null) {
            d.setApprovalConfigId(e.getApprovalConfig().getId());
            d.setApprovalConfigDisplay(e.getApprovalConfig().getCode() + " — " + e.getApprovalConfig().getName());
        }
        if (e.getCurrentApprovalLevel() != null) d.setCurrentApprovalLevelId(e.getCurrentApprovalLevel().getId());
        if (e.getCurrentApproverUser() != null) {
            d.setCurrentApproverUserId(e.getCurrentApproverUser().getId());
            d.setCurrentApproverDisplay(displayName(e.getCurrentApproverUser().getFullName(), e.getCurrentApproverUser().getUsername()));
        }
        // Load history
        d.setHistory(getHistory(e.getId()));
        return d;
    }

    @Override
    public ApprovalDelegationDTO toDTO(ApprovalDelegation e) {
        ApprovalDelegationDTO d = ApprovalDelegationDTO.builder()
            .id(e.getId()).delegationCode(e.getDelegationCode())
            .module(e.getModule()).documentType(e.getDocumentType())
            .startDate(e.getStartDate()).endDate(e.getEndDate())
            .maxAmount(e.getMaxAmount()).reason(e.getReason())
            .status(e.getStatus()).active(e.isActive()).notifyDelegator(e.isNotifyDelegator())
            .revocationReason(e.getRevocationReason())
            .revokedAt(e.getRevokedAt() != null ? e.getRevokedAt().toString() : null)
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
            .build();
        if (e.getDelegator() != null) { d.setDelegatorId(e.getDelegator().getId()); d.setDelegatorDisplay(displayName(e.getDelegator().getFullName(), e.getDelegator().getUsername())); }
        if (e.getDelegate() != null)  { d.setDelegateId(e.getDelegate().getId());   d.setDelegateDisplay(displayName(e.getDelegate().getFullName(), e.getDelegate().getUsername())); }
        if (e.getRevokedBy() != null) { d.setRevokedByDisplay(displayName(e.getRevokedBy().getFullName(), e.getRevokedBy().getUsername())); }
        return d;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private ApprovalConfig buildConfig(ApprovalConfigDTO dto, ApprovalConfig e) {
        e.setCode(dto.getCode().trim().toUpperCase());
        e.setName(dto.getName().trim());
        e.setDescription(dto.getDescription());
        e.setDocumentType(dto.getDocumentType());
        e.setModule(dto.getModule());
        e.setFlowType(dto.getFlowType() != null ? dto.getFlowType() : "SEQUENTIAL");
        e.setActive(Boolean.TRUE.equals(dto.getActive()));
        e.setEnableReminders(Boolean.TRUE.equals(dto.getEnableReminders()));
        e.setUseReportingHierarchy(Boolean.TRUE.equals(dto.getUseReportingHierarchy()));
        e.setPriority(dto.getPriority());
        e.setAutoEscalationHours(dto.getAutoEscalationHours());
        e.setReminderIntervalHours(dto.getReminderIntervalHours());
        e.setMinAmount(dto.getMinAmount());
        e.setMaxAmount(dto.getMaxAmount());
        setAudit(e, e.getId() == null);
        return e;
    }

    private void syncLevels(List<ApprovalConfigDTO.LevelDTO> dtos, ApprovalConfig parent) {
        if (dtos == null) return;
        parent.getLevels().clear();
        int num = 1;
        for (ApprovalConfigDTO.LevelDTO ld : dtos) {
            if (ld.getLevelName() == null || ld.getLevelName().isBlank()) continue;
            ApprovalLevel level = ApprovalLevel.builder()
                .approvalConfig(parent)
                .levelNumber(ld.getLevelNumber() != null ? ld.getLevelNumber() : num)
                .levelName(ld.getLevelName().trim())
                .description(ld.getDescription())
                .approverDescription(ld.getApproverDescription())
                .isActive(Boolean.TRUE.equals(ld.getActive()))
                .canApproveWithChanges(Boolean.TRUE.equals(ld.getCanApproveWithChanges()))
                .canDelegate(Boolean.TRUE.equals(ld.getCanDelegate()))
                .canForward(Boolean.TRUE.equals(ld.getCanForward()))
                .canHold(Boolean.TRUE.equals(ld.getCanHold()))
                .build();
            if (ld.getApproverUserId() != null) level.setApproverUser(userRepo.getReferenceById(ld.getApproverUserId()));
            setAudit(level, true);
            parent.getLevels().add(level);
            num++;
        }
        configRepo.save(parent);
    }

    private void recordHistory(ApprovalRequest req, ApprovalLevel level, Long actorUserId,
                               String action, String comments, String rejectionReason, String returnReason) {
        userRepo.findById(actorUserId).ifPresent(actor -> {
            ApprovalHistory h = ApprovalHistory.builder()
                .approvalRequest(req)
                .approvalLevel(level)
                .actorUser(actor)
                .levelNumber(req.getCurrentLevelNumber())
                .levelName(level != null ? level.getLevelName() : "System")
                .actorName(displayName(actor.getFullName(), actor.getUsername()))
                .action(action)
                .status(action)
                .comments(comments)
                .rejectionReason(rejectionReason)
                .returnReason(returnReason)
                .isAutoAction(false)
                .actionAt(LocalDateTime.now())
                .build();
            historyRepo.save(h);
        });
    }

    private void guardActionAllowed(ApprovalRequest req) {
        Long userId = ContextProvider.getCurrentUserId();
        if (req.getCurrentApproverUser() == null || !req.getCurrentApproverUser().getId().equals(userId))
            throw new IllegalStateException("You are not the current approver for this request.");
        if (!Set.of("IN_APPROVAL","SUBMITTED").contains(req.getStatus()))
            throw new IllegalStateException("Request status '" + req.getStatus() + "' does not allow actions.");
    }

    private void guardMutable(ApprovalRequest req) {
        if (Set.of("APPROVED","REJECTED","CANCELLED","COMPLETED").contains(req.getStatus()))
            throw new IllegalStateException("Request is already " + req.getStatus() + ".");
    }

    private ApprovalConfig findConfig(Long id) { return configRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Config #" + id + " not found.")); }
    private ApprovalRequest findRequest(Long id) { return requestRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Request #" + id + " not found.")); }
    private ApprovalDelegation findDelegation(Long id) { return delegationRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Delegation #" + id + " not found.")); }

    private String displayName(String fullName, String username) {
        return (fullName != null && !fullName.isBlank()) ? fullName : username;
    }

    private void setAudit(Object e, boolean isCreate) {
        String user = SecurityHelper.currentUsername().orElse("system");
        LocalDateTime now = LocalDateTime.now();
        if (e instanceof ApprovalConfig ac) { if (isCreate) { ac.setCreatedBy(user); ac.setCreatedAt(now); } ac.setUpdatedBy(user); ac.setUpdatedAt(now); }
        else if (e instanceof ApprovalLevel al) { if (isCreate) { al.setCreatedBy(user); al.setCreatedAt(now); } al.setUpdatedBy(user); al.setUpdatedAt(now); }
        else if (e instanceof ApprovalRequest ar) { if (isCreate) { ar.setCreatedBy(user); ar.setCreatedAt(now); } ar.setUpdatedBy(user); ar.setUpdatedAt(now); }
        else if (e instanceof ApprovalDelegation ad) { if (isCreate) { ad.setCreatedBy(user); ad.setCreatedAt(now); } ad.setUpdatedBy(user); ad.setUpdatedAt(now); }
    }
}
