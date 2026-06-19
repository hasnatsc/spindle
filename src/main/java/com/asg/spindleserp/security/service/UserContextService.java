package com.asg.spindleserp.security.service;

import com.asg.spindleserp.hrm.repository.EmployeeRepository;
import com.asg.spindleserp.security.dto.UserContextDTO;
import com.asg.spindleserp.security.entity.UserContext;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.organization.entity.*;
import com.asg.spindleserp.security.repository.UserContextRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.security.session.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UserContextService
 *
 * ══════════════════════════════════════════════════════════════════════
 * CALL FLOW
 * ══════════════════════════════════════════════════════════════════════
 *
 *  LOGIN
 *    LoginSuccessHandler
 *      → userRepository.findByUsernameWithAllContext()   [1 query: user + 4 allowed sets]
 *      → userContextService.loadContext(user)
 *          → userContextRepository.findByUserIdEager()   [1 query: ctx + org/BU/CC/WH]
 *          → fills UserContextDTO: active ctx + allowed scope lists + employee + prefs
 *          → contextHolder.set(dto)                      [stored in HTTP session]
 *
 *  EVERY REQUEST (e.g. new PO form loads)
 *    ContextProvider.getOrganizationId()                 [ZERO DB — reads from session]
 *    ContextProvider.getOrganizationReference()          [ZERO DB — JPA proxy, no SQL]
 *
 *  CONTEXT SWITCH (user clicks different org in top menu)
 *    UserContextController.switchOrganization(id)
 *      → updates UserContext row in DB
 *      → calls userContextService.loadContext(user)      [refresh session]
 *
 *  ADMIN SAVES USER WITH DEFAULT CONTEXT
 *    UserService.createUser() / updateUser()
 *      → userContextService.saveDefaultContext(userId, org, bu, cc, wh)
 *      → writes to user_context table
 *      → effective on user's next login (or immediate if session active + reloaded)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserContextRepository userContextRepository;
    private final UserContextHolder     contextHolder;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    // Approval repositories — uncomment once module is active
    // private final ApprovalRequestRepository      approvalRequestRepository;
    // private final ApprovalNotificationRepository approvalNotificationRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  LOAD FULL CONTEXT (called at login and after every switch)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds the complete UserContextDTO and stores it in the session.
     * The User passed in MUST have organizations/allowedBU/CC/WH loaded
     * (use userRepository.findByUsernameWithAllContext()).
     */
    @Transactional
    public void loadContext(User user) {

        UserContext ctx = userContextRepository.findByUserIdEager(user.getId()).orElseGet(() -> createEmptyContext(user));
        UserContextDTO dto = new UserContextDTO();
        // 1. Identity
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());

        // 2. Active working context (from the user_context row)
        if (ctx.getOrganization() != null) {
            dto.setOrganizationId(ctx.getOrganization().getId());
            dto.setOrganizationName(ctx.getOrganization().getName());
        }
        if (ctx.getBusinessUnit() != null) {
            dto.setBusinessUnitId(ctx.getBusinessUnit().getId());
            dto.setBusinessUnitName(ctx.getBusinessUnit().getName());
        }
        if (ctx.getCostCenter() != null) {
            dto.setCostCenterId(ctx.getCostCenter().getId());
            dto.setCostCenterName(ctx.getCostCenter().getCostCenterName());
        }
        if (ctx.getWarehouse() != null) {
            dto.setWarehouseId(ctx.getWarehouse().getId());
            dto.setWarehouseName(ctx.getWarehouse().getWarehouseName());
        }

        // 3. Allowed scopes (from the User's ManyToMany sets — loaded by findByUsernameWithAllContext)
        dto.setAllowedOrganizations(
            safeSet(user.getOrganizations()).stream()
                .map(o -> new UserContextDTO.ScopeItem(o.getId(), o.getCode(), o.getName()))
                .collect(Collectors.toList())
        );
        dto.setAllowedBusinessUnits(
            safeSet(user.getAllowedBusinessUnits()).stream()
                .map(bu -> new UserContextDTO.ScopeItem(bu.getId(), bu.getCode(), bu.getName(),
                        bu.getOrganization() != null ? bu.getOrganization().getId() : null))
                .collect(Collectors.toList())
        );
        dto.setAllowedCostCenters(
            safeSet(user.getAllowedCostCenters()).stream()
                .map(cc -> new UserContextDTO.ScopeItem(cc.getId(), cc.getCostCenterCode(), cc.getCostCenterName(),
                        cc.getBusinessUnit() != null ? cc.getBusinessUnit().getId() : null))
                .collect(Collectors.toList())
        );
        dto.setAllowedWarehouses(
            safeSet(user.getAllowedWarehouses()).stream()
                .map(wh -> new UserContextDTO.ScopeItem(wh.getId(), wh.getWarehouseCode(), wh.getWarehouseName(),
                        wh.getBusinessUnit() != null ? wh.getBusinessUnit().getId() : null))
                .collect(Collectors.toList())
        );

        // 4. Employee context (nullable — only if HRM module has this user linked)
        loadEmployeeContext(user, dto);

        // 5. Approval preferences from the context row
        mapApprovalPreferences(ctx, dto);

        // 6. Approval stats — uncomment once module exists
        // loadApprovalStats(user.getId(), dto);

        contextHolder.set(dto);
        contextHolder.markApprovalStatsRefreshed();

        log.info("Context loaded: user='{}' activeOrg='{}' activeBU='{}' allowedOrgs={}",
                user.getUsername(),
                dto.getOrganizationName() != null ? dto.getOrganizationName() : "—",
                dto.getBusinessUnitName()  != null ? dto.getBusinessUnitName()  : "—",
                dto.getAllowedOrganizations() != null ? dto.getAllowedOrganizations().size() : 0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SAVE DEFAULT CONTEXT (called by UserService from users-form submit)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Persists the admin-chosen default context for a user.
     * Takes effect on the user's next login (loadContext picks it up).
     * All four params may be null — null means "no default set".
     */
    @Transactional
    public void saveDefaultContext(Long userId, Organization organization, BusinessUnit businessUnit, CostCenter   costCenter, Warehouse    warehouse) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserContext ctx = userContextRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    UserContext c = new UserContext();
                    c.setUser(user);
                    // We don't need the User proxy here since userId is enough for the PK
                    return c;
                });
        ctx.setUser(user);
        ctx.setOrganization(organization);
        ctx.setBusinessUnit(businessUnit);
        ctx.setCostCenter(costCenter);
        ctx.setWarehouse(warehouse);
        userContextRepository.save(ctx);

        log.debug("Default context saved for userId={} org={} bu={} cc={} wh={}",
                userId,
                organization != null ? organization.getId() : null,
                businessUnit != null ? businessUnit.getId() : null,
                costCenter   != null ? costCenter.getId()   : null,
                warehouse    != null ? warehouse.getId()    : null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public UserContext getOrCreateContext(User user) {
        return userContextRepository.findByUserIdForUpdate(user.getId()).orElseGet(() -> createEmptyContext(user));
    }

    /**
     * Read-only lookup used by UserServiceImpl.toDTO() to populate the
     * defaultXxxId fields in the DTO (for pre-filling the edit form).
     */
    @Transactional(readOnly = true)
    public Optional<UserContext> findContextByUserId(Long userId) {
        return userContextRepository.findByUserIdEager(userId);
    }

    @Transactional
    public UserContext createEmptyContext(User user) {
        UserContext ctx = new UserContext();
        ctx.setUser(user);
        return userContextRepository.save(ctx);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  APPROVAL PREFERENCE UPDATES
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void updateNotificationPreferences(Long userId, String freq,
                                              Boolean email, Boolean sms,
                                              Boolean push, Boolean wa) {
        userContextRepository.updateNotificationPrefs(userId, freq, email, sms, push, wa);
        UserContextDTO dto = contextHolder.get();
        if (dto != null) {
            dto.setApprovalNotificationFrequency(freq);
            dto.setApprovalEmailEnabled(email);
            dto.setApprovalSmsEnabled(sms);
            dto.setApprovalPushEnabled(push);
            dto.setApprovalWhatsappEnabled(wa);
        }
    }

    @Transactional
    public void updateDisplayPreferences(Long userId, String view, Integer interval,
                                         Boolean sound, Boolean desktop, Boolean badge) {
        userContextRepository.updateDisplayPrefs(userId, view, interval, sound, desktop, badge);
        UserContextDTO dto = contextHolder.get();
        if (dto != null) {
            dto.setApprovalDefaultView(view);
            dto.setApprovalRefreshInterval(interval);
            dto.setApprovalSoundEnabled(sound);
            dto.setApprovalDesktopNotification(desktop);
            dto.setShowApprovalBadge(badge);
        }
        if (interval != null && interval > 0) contextHolder.setApprovalStatsCacheDuration(interval);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void loadEmployeeContext(User user, UserContextDTO dto) {
        try {
            employeeRepository.findByUserId(user.getId()).ifPresent(emp -> {
                dto.setEmployeeId(emp.getId());
                dto.setEmployeeName(emp.getFullName());
                dto.setEmployeeCode(emp.getEmployeeCode());
                if (emp.getDepartment()        != null) { dto.setDepartmentId(emp.getDepartment().getId());     dto.setDepartmentName(emp.getDepartment().getName()); }
                if (emp.getDesignation()       != null) { dto.setDesignationId(emp.getDesignation().getId());  dto.setDesignationName(emp.getDesignation().getDesignationName()); }
                if (emp.getReportingManager()  != null) { dto.setReportingManagerId(emp.getReportingManager().getId()); dto.setReportingManagerName(emp.getReportingManager().getFullName()); }
//                dto.setIsDepartmentHead(Boolean.TRUE.equals(emp.getIsDepartmentHead()));
//                dto.setCanApprove(Boolean.TRUE.equals(emp.getIsDepartmentHead()) || employeeRepository.countByReportingManagerId(emp.getId()) > 0);
            });
        } catch (Exception e) {
            log.warn("Employee context unavailable for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private void mapApprovalPreferences(UserContext ctx, UserContextDTO dto) {
        dto.setApprovalNotificationFrequency(coalesce(ctx.getApprovalNotificationFrequency(), "IMMEDIATE"));
        dto.setApprovalEmailEnabled(coalesce(ctx.getApprovalEmailEnabled(), Boolean.TRUE));
        dto.setApprovalSmsEnabled(coalesce(ctx.getApprovalSmsEnabled(), Boolean.FALSE));
        dto.setApprovalPushEnabled(coalesce(ctx.getApprovalPushEnabled(), Boolean.TRUE));
        dto.setApprovalWhatsappEnabled(coalesce(ctx.getApprovalWhatsappEnabled(), Boolean.FALSE));
        dto.setApprovalDefaultView(coalesce(ctx.getApprovalDefaultView(), "PENDING"));
        dto.setApprovalRefreshInterval(coalesce(ctx.getApprovalRefreshInterval(), 60));
        dto.setApprovalSoundEnabled(coalesce(ctx.getApprovalSoundEnabled(), Boolean.TRUE));
        dto.setApprovalDesktopNotification(coalesce(ctx.getApprovalDesktopNotification(), Boolean.TRUE));
        dto.setShowApprovalBadge(coalesce(ctx.getShowApprovalBadge(), Boolean.TRUE));
        dto.setLastViewedNotificationId(ctx.getLastViewedNotificationId());
    }

    private <T> T coalesce(T v, T fallback) { return v != null ? v : fallback; }

    private <T> java.util.Set<T> safeSet(java.util.Set<T> s) {
        return s != null ? s : Collections.emptySet();
    }
}
