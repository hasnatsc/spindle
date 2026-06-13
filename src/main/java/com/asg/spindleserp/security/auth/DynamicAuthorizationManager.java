package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.entity.Permission;
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

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Runtime authorization manager — database-driven, zero DB hits per request.
 *
 * ROOT CAUSE of the compile error:
 *   Spring Security 7 defines the interface as:
 *     AuthorizationDecision authorize(Supplier<? extends Authentication> auth, T object)
 *
 *   The wildcard is "? extends Authentication" — NOT plain "Authentication".
 *   Java generics treats Supplier<Authentication> as a DIFFERENT type from
 *   Supplier<? extends Authentication>, so @Override fails with
 *   "does not override abstract method authorize(Supplier<? extends Authentication>, T)".
 *
 *   Fix: the overriding method must use the exact wildcard → Supplier<? extends Authentication>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionRepository permissionRepository;
    private final AntPathMatcher       pathMatcher = new AntPathMatcher();

    // ── Permission URL-pattern cache (5-minute TTL) ───────────────────────
    private volatile List<Permission> permissionCache;
    private volatile long             cacheLoadedAt = 0L;
    private static final long         CACHE_TTL_MS  = 5 * 60 * 1_000L;

    /**
     * ✅ CORRECT signature for Spring Security 7:
     *    Supplier<? extends Authentication>  — wildcard is required.
     *    Using Supplier<Authentication> fails the @Override check.
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

        // ── 3. User's permission names (in memory — built at login, no DB) ─
        Set<String> userPermNames = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        // ── 4. Match against cached permission URL patterns ───────────────
        boolean allowed = getCachedPermissions().stream()
                .filter(Permission::isActive)
                .filter(p -> userPermNames.contains(p.getName()))
                .anyMatch(p -> urlMatches(p, uri, method));

        if (log.isDebugEnabled()) {
            log.debug("AUTHZ {} {} user='{}' → {}",
                    method, uri, auth.getName(), allowed ? "GRANT" : "DENY");
        }

        return new AuthorizationDecision(allowed);
    }

    // ── URL matching ──────────────────────────────────────────────────────

    private boolean urlMatches(Permission p, String uri, String method) {
        if (p.getUrlPattern() == null || p.getUrlPattern().isBlank()) {
            return false;
        }
        if (p.getHttpMethod() != null && !p.getHttpMethod().isBlank()
                && !p.getHttpMethod().equalsIgnoreCase(method)) {
            return false;
        }
        return pathMatcher.match(p.getUrlPattern(), uri);
    }

    // ── TTL permission cache ──────────────────────────────────────────────

    private List<Permission> getCachedPermissions() {
        long now = System.currentTimeMillis();
        if (permissionCache == null || (now - cacheLoadedAt) > CACHE_TTL_MS) {
            synchronized (this) {
                if (permissionCache == null || (now - cacheLoadedAt) > CACHE_TTL_MS) {
                    permissionCache = permissionRepository.findAllActive();
                    cacheLoadedAt   = now;
                    log.info("Permission cache refreshed ({} entries)", permissionCache.size());
                }
            }
        }
        return permissionCache;
    }

    /** Force immediate reload — call this after any Permission is saved/updated. */
    public void invalidateCache() {
        synchronized (this) {
            permissionCache = null;
            cacheLoadedAt   = 0L;
        }
        log.info("Permission cache invalidated");
    }
}
