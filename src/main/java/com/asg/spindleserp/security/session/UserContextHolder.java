package com.asg.spindleserp.security.session;

import com.asg.spindleserp.security.dto.UserContextDTO;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * UserContextHolder — session-scoped, serializable.
 *
 * Spring creates ONE instance per HTTP session and serializes it into
 * the SPRING_SESSION_ATTRIBUTES table (Spring Session JDBC).
 * ScopedProxyMode.TARGET_CLASS lets singletons (@Service, @Controller)
 * inject this via a thread-safe proxy.
 *
 * All reads are zero-DB — everything is cached in the UserContextDTO.
 * The DTO is refreshed only at login and after a context switch.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContextHolder implements Serializable {

    private static final long serialVersionUID = 1L;

    private UserContextDTO context = new UserContextDTO();

    // ── Approval stats cache TTL ───────────────────────────────────────────
    private LocalDateTime approvalStatsLastRefreshed;
    private int approvalStatsCacheDuration = 60;   // seconds

    // ── Core ──────────────────────────────────────────────────────────────

    public UserContextDTO get()              { return context; }
    public void set(UserContextDTO dto)      { this.context = dto; }
    public void clear()                      { this.context = new UserContextDTO(); this.approvalStatsLastRefreshed = null; }

    // ── Approval stats cache ───────────────────────────────────────────────

    public boolean needsApprovalStatsRefresh() {
        return approvalStatsLastRefreshed == null
            || LocalDateTime.now().isAfter(
                   approvalStatsLastRefreshed.plusSeconds(approvalStatsCacheDuration));
    }

    public void markApprovalStatsRefreshed()       { this.approvalStatsLastRefreshed = LocalDateTime.now(); }
    public void invalidateApprovalStatsCache()      { this.approvalStatsLastRefreshed = null; }
    public void setApprovalStatsCacheDuration(int s){ this.approvalStatsCacheDuration = s; }
    public int  getApprovalStatsCacheDuration()     { return approvalStatsCacheDuration; }

    // ── Approval counter helpers (in-memory increment/decrement) ──────────

    public void updateApprovalStats(Integer pending, Integer overdue,
                                    Integer unread, Integer delegFrom, Integer delegTo) {
        if (context != null) {
            if (pending   != null) context.setPendingApprovalsCount(pending);
            if (overdue   != null) context.setOverdueApprovalsCount(overdue);
            if (unread    != null) context.setUnreadNotificationsCount(unread);
            if (delegFrom != null) context.setActiveDelegationsFromMe(delegFrom);
            if (delegTo   != null) context.setActiveDelegationsToMe(delegTo);
            markApprovalStatsRefreshed();
        }
    }

    public void incrementPendingCount() {
        if (context != null && context.getPendingApprovalsCount() != null)
            context.setPendingApprovalsCount(context.getPendingApprovalsCount() + 1);
    }

    public void decrementPendingCount() {
        if (context != null && context.getPendingApprovalsCount() != null
                && context.getPendingApprovalsCount() > 0)
            context.setPendingApprovalsCount(context.getPendingApprovalsCount() - 1);
    }

    public void incrementUnreadNotifications() {
        if (context != null && context.getUnreadNotificationsCount() != null)
            context.setUnreadNotificationsCount(context.getUnreadNotificationsCount() + 1);
    }

    // ── Direct getters (convenience for templates / controllers) ──────────

    public Long    getOrganizationId()         { return context != null ? context.getOrganizationId()             : null; }
    public Long    getBusinessUnitId()          { return context != null ? context.getBusinessUnitId()              : null; }
    public Long    getCostCenterId()            { return context != null ? context.getCostCenterId()               : null; }
    public Long    getWarehouseId()             { return context != null ? context.getWarehouseId()                : null; }
    public String  getUsername()               { return context != null ? context.getUsername()                   : null; }
    public Long    getEmployeeId()             { return context != null ? context.getEmployeeId()                 : null; }
    public Integer getPendingApprovalsCount()  { return context != null ? coalesce(context.getPendingApprovalsCount(),  0) : 0; }
    public Integer getUnreadNotificationsCount(){ return context != null ? coalesce(context.getUnreadNotificationsCount(), 0) : 0; }
    public boolean canApprove()                { return context != null && Boolean.TRUE.equals(context.getCanApprove()); }
    public boolean isDepartmentHead()          { return context != null && Boolean.TRUE.equals(context.getIsDepartmentHead()); }

    private <T> T coalesce(T v, T fallback)    { return v != null ? v : fallback; }
}
