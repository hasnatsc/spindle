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
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ContextProvider — static utility for reading the current user's context.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHAT CHANGED, AND WHY (two fixes; the rest of the class is untouched)
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ── FIX 1 — ctx() blew up on any non-request thread. ★ LATENT CRASH ───────
 *
 * The old ctx() was:
 *
 *     private static UserContextDTO ctx() { return holder != null ? holder.get() : null; }
 *
 * `holder` is a session-scoped bean behind a ScopedProxyMode.TARGET_CLASS
 * proxy. Calling .get() on that proxy outside a servlet request throws:
 *
 *     IllegalStateException: No thread-bound request found: Are you referring
 *     to request attributes outside of an actual web request…?
 *
 * The null-check on `holder` did not help — the proxy is never null; it is the
 * *resolution* of the proxy that fails. And this is not hypothetical:
 *
 *     EcCustomer.java, line ~57:
 *         @Builder.Default
 *         @Column(name = "organization_id", nullable = false, updatable = false)
 *         private Long organizationId = ContextProvider.getOrganizationId();
 *
 * Hibernate instantiates every entity through its no-arg constructor, and
 * Lombok's @Builder.Default keeps the field initialiser in that constructor.
 * So ContextProvider.getOrganizationId() fires on EVERY SINGLE LOAD of an
 * EcCustomer row — including loads on threads that have no request bound to
 * them. pom.xml pulls in spring-boot-starter-batch-jdbc; the moment any batch
 * job, @Async method, scheduled task or CompletableFuture touches an
 * EcCustomer, that entity load throws. (Hibernate overwrites the field with
 * the real column value a microsecond later, which is why this is pure
 * downside: the call is useless on the load path AND it is a landmine.)
 *
 * FIX: ctx() now checks RequestContextHolder first and returns null off-thread
 * instead of throwing. Behaviour inside a request is byte-for-byte identical.
 *
 * ── FIX 2 — anonymous storefront org resolution no longer needs a session ──
 *
 * ContextProvider.getOrganizationId() could only ever be populated by
 * UserContextService.loadContext(), which only runs at ERP staff login. An
 * anonymous shopper never goes through /login, so StorefrontOrgContextFilter
 * seeds the org into the session-scoped holder for them.
 *
 * That works, but it forces a session-scoped bean to materialise, which in turn
 * FORCES AN HTTP SESSION into existence, on the very first request from any
 * anonymous visitor — including every crawler, uptime probe and bot. With
 * spring.session.store-type=jdbc that is one INSERT into SPRING_SESSION and
 * SPRING_SESSION_ATTRIBUTES per visitor, retained for 30 minutes.
 *
 * A request-scoped fallback (below) lets the filter hand the org to this
 * request WITHOUT touching the session. Precedence is unchanged and strictly
 * additive:
 *
 *     1. session UserContextHolder   ← real ERP staff context. Always wins.
 *     2. request-scoped anonymous org ← storefront/travel-site visitors.
 *     3. null
 *
 * Nothing that worked before behaves differently; the fallback only fires
 * where the answer used to be null.
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ZERO DATABASE PER CALL (unchanged)
 *   All data was loaded at login by UserContextService.loadContext() and
 *   cached in UserContextHolder. Every static getter below reads memory.
 *   Exceptions, as before:
 *     getOrganizationReference()   → getReferenceById() = JPA proxy, no SQL
 *     getCurrentUser() / getUser() → findById() fires ONE SELECT
 *     getCurrentUserRoles()        → findByIdWithRoles() fires ONE SELECT
 */
@Component
public class ContextProvider {

    // ── Static backplane (set once at startup via @PostConstruct) ────────────
    private static UserContextHolder      holder;
    private static OrganizationRepository orgRepo;
    private static BusinessUnitRepository buRepo;
    private static CostCenterRepository   ccRepo;
    private static WarehouseRepository    whRepo;
    private static UserRepository         userRepo;

    /**
     * Request-scoped organisation for visitors who have no ERP session context
     * (anonymous storefront / travel-site). Set and cleared per request by
     * StorefrontOrgContextFilter — always inside a try/finally, so it can never
     * leak across pooled request threads.
     */
    private static final ThreadLocal<AnonymousOrg> ANONYMOUS_ORG = new ThreadLocal<>();

    /** Immutable carrier for the anonymous org id + display name. */
    public record AnonymousOrg(Long id, String name) {}

    // ── Injected instance fields ─────────────────────────────────────────────
    private final UserContextHolder      _holder;
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

    // ══════════════════════════════════════════════════════════════════════
    // ANONYMOUS ORG — request-scoped fallback (StorefrontOrgContextFilter only)
    // ══════════════════════════════════════════════════════════════════════

    /** Called by StorefrontOrgContextFilter at the top of a storefront request. */
    public static void setAnonymousOrganization(Long id, String name) {
        if (id == null) { ANONYMOUS_ORG.remove(); return; }
        ANONYMOUS_ORG.set(new AnonymousOrg(id, name));
    }

    /** MUST be called from a finally block — request threads are pooled. */
    public static void clearAnonymousOrganization() {
        ANONYMOUS_ORG.remove();
    }

    private static AnonymousOrg anon() {
        return ANONYMOUS_ORG.get();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORE ACCESSOR
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIX 1: never resolve the session-scoped proxy off a request thread.
     * Returns null instead of throwing IllegalStateException, so entity
     * hydration on a batch/async thread cannot blow up.
     */
    private static UserContextDTO ctx() {
        if (holder == null) return null;
        if (RequestContextHolder.getRequestAttributes() == null) return null;
        try {
            return holder.get();
        } catch (RuntimeException e) {
            // Defensive: a request that is already completing (async dispatch
            // teardown) can still fail proxy resolution. Never let a read of
            // ambient context break the caller.
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ORGANIZATION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Active org ID — zero DB.
     * Precedence: ERP session context → anonymous storefront org → null.
     */
    public static Long getOrganizationId() {
        UserContextDTO c = ctx();
        if (c != null && c.getOrganizationId() != null) return c.getOrganizationId();
        AnonymousOrg a = anon();
        return a != null ? a.id() : null;
    }

    /** JPA reference proxy — cheap FK assignment, NO SQL fired. */
    public static Organization getOrganizationReference() {
        Long id = getOrganizationId();
        return (id != null && orgRepo != null) ? orgRepo.getReferenceById(id) : null;
    }

    public static String getOrganizationName() {
        UserContextDTO c = ctx();
        if (c != null && c.getOrganizationName() != null) return c.getOrganizationName();
        AnonymousOrg a = anon();
        return a != null ? a.name() : null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BUSINESS UNIT
    // ══════════════════════════════════════════════════════════════════════

    public static Long getBusinessUnitId() {
        UserContextDTO c = ctx(); return c != null ? c.getBusinessUnitId() : null;
    }

    public static BusinessUnit getBusinessUnitReference() {
        Long id = getBusinessUnitId();
        return (id != null && buRepo != null) ? buRepo.getReferenceById(id) : null;
    }

    public static String getBusinessUnitName() {
        UserContextDTO c = ctx(); return c != null ? c.getBusinessUnitName() : null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // COST CENTER
    // ══════════════════════════════════════════════════════════════════════

    public static Long getCostCenterId() {
        UserContextDTO c = ctx(); return c != null ? c.getCostCenterId() : null;
    }

    public static CostCenter getCostCenterReference() {
        Long id = getCostCenterId();
        return (id != null && ccRepo != null) ? ccRepo.getReferenceById(id) : null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // WAREHOUSE
    // ══════════════════════════════════════════════════════════════════════

    public static Long getWarehouseId() {
        UserContextDTO c = ctx(); return c != null ? c.getWarehouseId() : null;
    }

    public static Warehouse getWarehouseReference() {
        Long id = getWarehouseId();
        if (id == null) return null;
        return whRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Warehouse #" + id + " not found."));
    }

    // ══════════════════════════════════════════════════════════════════════
    // USER
    // ══════════════════════════════════════════════════════════════════════

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
        Long id = getUserId();
        return (id != null && userRepo != null) ? userRepo.getReferenceById(id) : null;
    }

    /** Fully loaded User — fires ONE SELECT. Use only when you need to read fields. */
    public static User getCurrentUser() {
        Long id = getUserId();
        return (id != null && userRepo != null) ? userRepo.findById(id).orElse(null) : null;
    }

    /** Alias for getCurrentUser(). */
    public static User getUser() { return getCurrentUser(); }

    /**
     * Role names for the current user — fires ONE SELECT.
     * Returns empty list (never null) when context is not loaded.
     */
    public static List<String> getCurrentUserRoles() {
        Long id = getUserId();
        if (id == null || userRepo == null) return Collections.emptyList();
        return userRepo.findByIdWithRoles(id)
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // ══════════════════════════════════════════════════════════════════════
    // APPROVAL SHORTCUTS
    // ══════════════════════════════════════════════════════════════════════

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
