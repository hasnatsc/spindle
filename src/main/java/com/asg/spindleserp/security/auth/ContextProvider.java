package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.organization.entity.*;
import com.asg.spindleserp.organization.repository.*;
import com.asg.spindleserp.security.dto.UserContextDTO;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.security.session.UserContextHolder;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ContextProvider — static utility for reading the current user's context.
 *
 * ══════════════════════════════════════════════════════════════════════
 * ZERO DATABASE PER CALL
 * ══════════════════════════════════════════════════════════════════════
 * All data was loaded at login by UserContextService.loadContext() and
 * cached in UserContextHolder (session-scoped). Every static method
 * below just reads from that in-memory DTO.
 *
 * The only exceptions are:
 *   getOrganizationReference()  → getReferenceById() = JPA proxy, no SQL
 *   getCurrentUser() / getUser() → findById() fires ONE SELECT (use sparingly)
 *   getCurrentUserRoles()        → findByIdWithRoles() fires ONE SELECT
 *
 * ══════════════════════════════════════════════════════════════════════
 * USAGE EXAMPLES
 * ══════════════════════════════════════════════════════════════════════
 *   // In a service — seed FK fields on a new entity:
 *   entity.setOrganization(ContextProvider.getOrganizationReference());
 *   entity.setBusinessUnit(ContextProvider.getBusinessUnitReference());
 *   entity.setCreatedBy(ContextProvider.getCurrentUsername());
 *
 *   // In a service — filter a query:
 *   Long orgId = ContextProvider.getOrganizationId();
 *
 *   // In a service — get full user object (fires DB):
 *   User me = ContextProvider.getCurrentUser();
 */
@Component
public class ContextProvider {

    // Static backplane (set once at startup via @PostConstruct)
    private static UserContextHolder     holder;
    private static OrganizationRepository orgRepo;
    private static BusinessUnitRepository buRepo;
    private static CostCenterRepository   ccRepo;
    private static WarehouseRepository    whRepo;
    private static UserRepository         userRepo;

    // Injected instance fields
    private final UserContextHolder     _holder;
    private final OrganizationRepository _orgRepo;
    private final BusinessUnitRepository _buRepo;
    private final CostCenterRepository   _ccRepo;
    private final WarehouseRepository    _whRepo;
    private final UserRepository         _userRepo;

    public ContextProvider(UserContextHolder holder,
                           OrganizationRepository orgRepo,
                           BusinessUnitRepository buRepo,
                           CostCenterRepository ccRepo,
                           WarehouseRepository whRepo,
                           UserRepository userRepo) {
        this._holder   = holder;
        this._orgRepo  = orgRepo;
        this._buRepo   = buRepo;
        this._ccRepo   = ccRepo;
        this._whRepo   = whRepo;
        this._userRepo = userRepo;
    }

    @PostConstruct
    private void init() {
        ContextProvider.holder   = _holder;
        ContextProvider.orgRepo  = _orgRepo;
        ContextProvider.buRepo   = _buRepo;
        ContextProvider.ccRepo   = _ccRepo;
        ContextProvider.whRepo   = _whRepo;
        ContextProvider.userRepo = _userRepo;
    }

    // ── Private shortcut ─────────────────────────────────────────────────
    private static UserContextDTO ctx() { return holder != null ? holder.get() : null; }

    // ── Organization ──────────────────────────────────────────────────────

    /** Active org ID from session — zero DB. */
    public static Long getOrganizationId() {
        UserContextDTO c = ctx(); return c != null ? c.getOrganizationId() : null;
    }

    /** JPA reference proxy — cheap FK assignment, NO SQL fired. */
    public static Organization getOrganizationReference() {
        Long id = getOrganizationId(); return id != null ? orgRepo.getReferenceById(id) : null;
    }

    public static String getOrganizationName() {
        UserContextDTO c = ctx(); return c != null ? c.getOrganizationName() : null;
    }

    // ── Business Unit ─────────────────────────────────────────────────────

    public static Long getBusinessUnitId() {
        UserContextDTO c = ctx(); return c != null ? c.getBusinessUnitId() : null;
    }

    public static BusinessUnit getBusinessUnitReference() {
        Long id = getBusinessUnitId(); return id != null ? buRepo.getReferenceById(id) : null;
    }

    public static String getBusinessUnitName() {
        UserContextDTO c = ctx(); return c != null ? c.getBusinessUnitName() : null;
    }

    // ── Cost Center ───────────────────────────────────────────────────────

    public static Long getCostCenterId() {
        UserContextDTO c = ctx(); return c != null ? c.getCostCenterId() : null;
    }

    public static CostCenter getCostCenterReference() {
        Long id = getCostCenterId(); return id != null ? ccRepo.getReferenceById(id) : null;
    }

    // ── Warehouse ─────────────────────────────────────────────────────────

    public static Long getWarehouseId() {
        UserContextDTO c = ctx(); return c != null ? c.getWarehouseId() : null;
    }

    public static Warehouse getWarehouseReference() {
        Long id = getWarehouseId();
        if (id == null) return null;
        return whRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Warehouse #" + id + " not found."));
    }

    // ── User ──────────────────────────────────────────────────────────────

    public static Long getUserId() {
        UserContextDTO c = ctx(); return c != null ? c.getUserId() : null;
    }

    /** Alias for getUserId() — used by ApprovalServiceImpl. */
    public static Long getCurrentUserId() { return getUserId(); }

    public static String getUsername() {
        UserContextDTO c = ctx(); return c != null ? c.getUsername() : null;
    }

    /** Never null — falls back to "SYSTEM" for audit fields. */
    public static String getCurrentUsername() {
        UserContextDTO c = ctx();
        return (c != null && c.getUsername() != null) ? c.getUsername() : "SYSTEM";
    }

    /** JPA reference proxy — cheap, no SQL. Use only for FK assignment. */
    public static User getUserReference() {
        Long id = getUserId(); return id != null ? userRepo.getReferenceById(id) : null;
    }

    /** Fully loaded User — fires ONE SELECT. Use only when you need to read fields. */
    public static User getCurrentUser() {
        Long id = getUserId(); return id != null ? userRepo.findById(id).orElse(null) : null;
    }

    /** Alias for getCurrentUser(). */
    public static User getUser() { return getCurrentUser(); }

    /**
     * Role names for the current user — fires ONE SELECT.
     * Returns empty list (never null) when context is not loaded.
     */
    public static List<String> getCurrentUserRoles() {
        Long id = getUserId();
        if (id == null) return Collections.emptyList();
        return userRepo.findByIdWithRoles(id)
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // ── Approval shortcuts ─────────────────────────────────────────────────

    public static Integer getPendingApprovalsCount() {
        UserContextDTO c = ctx();
        return (c != null && c.getPendingApprovalsCount() != null) ? c.getPendingApprovalsCount() : 0;
    }

    public static Integer getUnreadNotificationsCount() {
        UserContextDTO c = ctx();
        return (c != null && c.getUnreadNotificationsCount() != null) ? c.getUnreadNotificationsCount() : 0;
    }

    public static boolean canApprove() {
        UserContextDTO c = ctx(); return c != null && Boolean.TRUE.equals(c.getCanApprove());
    }
}
