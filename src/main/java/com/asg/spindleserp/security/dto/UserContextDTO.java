package com.asg.spindleserp.security.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * UserContextDTO — lives in the HTTP session via UserContextHolder.
 *
 * Loaded ONCE at login by UserContextService.loadContext().
 * All reads after that via ContextProvider.getXxx() — zero DB per request.
 *
 * Contains:
 *   1. Identity          — userId, username, fullName
 *   2. Active context    — currently selected org/BU/CC/WH (IDs + names)
 *   3. Allowed scopes    — what the user can switch to in the top menu
 *   4. Employee context  — HRM link (nullable)
 *   5. Approval stats    — pending count, notifications etc.
 *   6. Approval prefs    — notification/display settings
 */
@Getter
@Setter
@NoArgsConstructor
public class UserContextDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── 1. Identity ───────────────────────────────────────────────────────
    private Long   userId;
    private String username;
    private String fullName;

    // ── 2. Active working context ─────────────────────────────────────────
    private Long   organizationId;
    private String organizationName;
    private Long   businessUnitId;
    private String businessUnitName;
    private Long   costCenterId;
    private String costCenterName;
    private Long   warehouseId;
    private String warehouseName;

    // ── 3. Allowed scopes (for top-menu dropdowns) ────────────────────────
    // Populated from User.organizations / allowedBusinessUnits etc. at login.
    // Stored in session → the top-menu endpoint reads from here, not the DB.
    private List<ScopeItem> allowedOrganizations;
    private List<ScopeItem> allowedBusinessUnits;
    private List<ScopeItem> allowedCostCenters;
    private List<ScopeItem> allowedWarehouses;

    /**
     * Lightweight item used for dropdown rendering.
     * parentId carries orgId for BU, buId for CC/WH — used for client-side cascade.
     */
    @Getter @Setter @NoArgsConstructor
    public static class ScopeItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long   id;
        private String code;
        private String name;
        private Long   parentId;

        public ScopeItem(Long id, String code, String name) {
            this.id = id; this.code = code; this.name = name;
        }
        public ScopeItem(Long id, String code, String name, Long parentId) {
            this(id, code, name); this.parentId = parentId;
        }
    }

    // ── 4. Employee context ───────────────────────────────────────────────
    private Long    employeeId;
    private String  employeeName;
    private String  employeeCode;
    private Long    departmentId;
    private String  departmentName;
    private Long    designationId;
    private String  designationName;
    private Long    reportingManagerId;
    private String  reportingManagerName;
    private Boolean isDepartmentHead;
    private Boolean canApprove;

    // ── 5. Approval stats ─────────────────────────────────────────────────
    private Integer pendingApprovalsCount    = 0;
    private Integer overdueApprovalsCount    = 0;
    private Integer unreadNotificationsCount = 0;
    private Integer activeDelegationsFromMe  = 0;
    private Integer activeDelegationsToMe    = 0;
    private Integer myPendingRequestsCount   = 0;
    private Integer recentlyApprovedCount    = 0;
    private Integer recentlyRejectedCount    = 0;
    private Integer todayApprovedByMe        = 0;
    private Integer todayRejectedByMe        = 0;
    private Double  averageResponseTimeHours;
    private String  urgencyStatus;
    private Map<String, Integer> pendingByModule;
    private List<Object> recentPendingApprovals;
    private List<Object> recentNotifications;

    // ── 6. Approval preferences ───────────────────────────────────────────
    private String  approvalNotificationFrequency = "IMMEDIATE";
    private Boolean approvalEmailEnabled          = Boolean.TRUE;
    private Boolean approvalSmsEnabled            = Boolean.FALSE;
    private Boolean approvalPushEnabled           = Boolean.TRUE;
    private Boolean approvalWhatsappEnabled       = Boolean.FALSE;
    private String  approvalDefaultView           = "PENDING";
    private Integer approvalRefreshInterval       = 60;
    private Boolean approvalSoundEnabled          = Boolean.TRUE;
    private Boolean approvalDesktopNotification   = Boolean.TRUE;
    private Boolean showApprovalBadge             = Boolean.TRUE;
    private Long    lastViewedNotificationId;
}
