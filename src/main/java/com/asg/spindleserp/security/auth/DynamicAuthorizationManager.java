package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.entity.Permission;
import com.asg.spindleserp.security.repository.OrgModuleRepository;
import com.asg.spindleserp.security.repository.PermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * DynamicAuthorizationManager — database-driven, zero DB hits per request.
 *
 * ══════════════════════════════════════════════════════════════════════
 * MULTI-TENANT ORG-MODULE CHECK (new in this version)
 * ══════════════════════════════════════════════════════════════════════
 *
 * Authorization now runs in TWO layers:
 *
 * Layer 1 — Permission check (existing):
 *   Does this user have a permission whose urlPattern matches this URI?
 *
 * Layer 2 — Org-module check (new):
 *   Does the user's organization have the matching module ACTIVE?
 *
 * Both must pass for GRANT. Super admin skips both.
 *
 * ── Why layer 2 is needed ──────────────────────────────────────────────
 * An org-admin could assign a PURCHASE permission to a user in an org
 * that has PURCHASE disabled. Without the module check, that user would
 * still get access. The org-module check is the authoritative gate.
 *
 * ── Cache design ──────────────────────────────────────────────────────
 * Permissions cache: same 5-minute TTL as before (List<Permission>).
 * Org-module cache: same 5-minute TTL (Map<Long orgId, Set<String>>).
 *
 * Both caches are invalidated together via invalidateCache().
 * OrgModuleService calls invalidateCache() after every module toggle.
 *
 * ── Performance ───────────────────────────────────────────────────────
 * The org-module cache is a ConcurrentHashMap<Long, Set<String>>.
 * The set lookup for a given orgId is O(1) (hash lookup).
 * Total per-request overhead: 2 hash lookups + the existing AntPath match.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionRepository permissionRepository;
    private final OrgModuleRepository  orgModuleRepository;
    private final AntPathMatcher       pathMatcher = new AntPathMatcher();

    // ── Permission cache ─────────────────────────────────────────────────
    private volatile List<Permission>            permissionCache;
    private volatile long                        permCacheLoadedAt = 0L;

    // ── Org-module cache ─────────────────────────────────────────────────
    // Key: orgId, Value: set of ACTIVE moduleKeys (uppercase) for that org.
    // CORE_SECURITY is always included for every org.
    private volatile Map<Long, Set<String>>      orgModuleCache;
    private volatile long                        omCacheLoadedAt   = 0L;

    private static final long CACHE_TTL_MS = 5 * 60 * 1_000L;

    /**
     * Spring Security 7 — Supplier<? extends Authentication> wildcard required.
     *
     * Decision flow:
     *   1. Not authenticated → DENY
     *   2. SUPER_ADMIN       → GRANT (bypasses all checks)
     *   3. Permission check  → does any of user's permissions match this URI?
     *   4. Org-module check  → is that permission's module active for user's org?
     *   5. Both pass         → GRANT, else DENY
     */
    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {

        Authentication auth = authenticationSupplier.get();

        // ── 1. Not authenticated ──────────────────────────────────────────
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return new AuthorizationDecision(false);
        }

        // ── 2. SUPER_ADMIN bypasses all URL checks ────────────────────────
        boolean superAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a) || "SUPER_ADMIN".equals(a));

        if (superAdmin) {
            return new AuthorizationDecision(true);
        }

        HttpServletRequest request = context.getRequest();
        String uri    = request.getRequestURI();
        String method = request.getMethod();

        // ── 3. User's permission names (in-memory, built at login) ────────
        Set<String> userPermNames = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        // ── 4. Org-module set for this user's org ─────────────────────────
        Long orgId = null;
        if (auth.getPrincipal() instanceof CustomUserDetails ud) {
            orgId = ud.getOrganizationId();
        }
        Set<String> activeModules = getActiveModulesForOrg(orgId);

        // ── 5. Match: permission matches URI AND module is active for org ──
        boolean allowed = getCachedPermissions().stream()
                .filter(Permission::isActive)
                .filter(p -> userPermNames.contains(p.getName()))
                .filter(p -> isModuleAllowed(p, activeModules))
                .anyMatch(p -> urlMatches(p, uri, method));

        if (log.isDebugEnabled()) {
            log.debug("AUTHZ {} {} user='{}' org={} → {}",
                    method, uri, auth.getName(), orgId, allowed ? "GRANT" : "DENY");
        }

        return new AuthorizationDecision(allowed);
    }

    // ── URL matching ──────────────────────────────────────────────────────

    private boolean urlMatches(Permission p, String uri, String method) {
        if (p.getUrlPattern() == null || p.getUrlPattern().isBlank()) return false;
        if (p.getHttpMethod() != null && !p.getHttpMethod().isBlank()
                && !p.getHttpMethod().equalsIgnoreCase(method)) return false;
        return pathMatcher.match(p.getUrlPattern(), uri);
    }

    // ── Org-module enforcement ────────────────────────────────────────────

    /**
     * Returns true if the permission's module is active for the org.
     *
     * Rules:
     *   - If permission has no module set → allowed (treat as CORE).
     *   - If org has no rows in the cache (new org, no modules configured yet)
     *     → DENY by default. Super admin must explicitly enable modules.
     *   - CORE_SECURITY is always in the active set.
     */
    private boolean isModuleAllowed(Permission p, Set<String> activeModules) {
        String module = p.getModule();
        if (module == null || module.isBlank()) return true; // unclassified → allow
        return activeModules.contains(module.toUpperCase());
    }

    /**
     * Returns the set of active modules for the given org from cache.
     * If orgId is null or org has no module config → returns only CORE_SECURITY.
     */
    private Set<String> getActiveModulesForOrg(Long orgId) {
        if (orgId == null) return Set.of("CORE_SECURITY");
        Map<Long, Set<String>> cache = getCachedOrgModules();
        return cache.getOrDefault(orgId, Set.of("CORE_SECURITY"));
    }

    // ── Caches ────────────────────────────────────────────────────────────

    private List<Permission> getCachedPermissions() {
        long now = System.currentTimeMillis();
        if (permissionCache == null || (now - permCacheLoadedAt) > CACHE_TTL_MS) {
            synchronized (this) {
                if (permissionCache == null || (now - permCacheLoadedAt) > CACHE_TTL_MS) {
                    permissionCache   = permissionRepository.findAllActive();
                    permCacheLoadedAt = now;
                    log.info("Permission cache refreshed ({} entries)", permissionCache.size());
                }
            }
        }
        return permissionCache;
    }

    private Map<Long, Set<String>> getCachedOrgModules() {
        long now = System.currentTimeMillis();
        if (orgModuleCache == null || (now - omCacheLoadedAt) > CACHE_TTL_MS) {
            synchronized (this) {
                if (orgModuleCache == null || (now - omCacheLoadedAt) > CACHE_TTL_MS) {
                    Map<Long, Set<String>> fresh = new ConcurrentHashMap<>();
                    orgModuleRepository.findAllActive().forEach(om -> {
                        fresh.computeIfAbsent(om.getOrganization().getId(), k -> new HashSet<>())
                             .add(om.getModuleKey().toUpperCase());
                    });
                    // CORE_SECURITY always granted for every org
                    fresh.values().forEach(s -> s.add("CORE_SECURITY"));
                    orgModuleCache  = fresh;
                    omCacheLoadedAt = now;
                    log.info("Org-module cache refreshed ({} orgs)", fresh.size());
                }
            }
        }
        return orgModuleCache;
    }

    /**
     * Force immediate reload of both caches.
     * Call after any Permission or OrgModule is saved/updated.
     */
    public void invalidateCache() {
        synchronized (this) {
            permissionCache   = null;
            permCacheLoadedAt = 0L;
            orgModuleCache    = null;
            omCacheLoadedAt   = 0L;
        }
        log.info("Authorization caches invalidated (permissions + org-modules)");
    }
}
