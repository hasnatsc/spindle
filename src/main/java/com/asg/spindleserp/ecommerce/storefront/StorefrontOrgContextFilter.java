// Path: com/asg/spindleserp/ecommerce/storefront/StorefrontOrgContextFilter.java
package com.asg.spindleserp.ecommerce.storefront;

import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.dto.UserContextDTO;
import com.asg.spindleserp.security.session.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * StorefrontOrgContextFilter — seeds the organisation context for visitors who
 * have no ERP login.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHY THIS EXISTS (unchanged from the original — the reasoning was correct)
 * ══════════════════════════════════════════════════════════════════════════
 * ContextProvider.getOrganizationId() is populated only by
 * UserContextService.loadContext(), which only runs at ERP staff login. An
 * anonymous shopper never goes through /login, so without this filter every
 * downstream call made on their behalf runs with orgId = NULL:
 *
 *   • StorefrontProductService.browse() interpolates orgId directly into the
 *     SQL text ("WHERE p.organization_id = " + orgId) → the literal string
 *     "= null" → a Postgres syntax error, not an empty result. Shop, category
 *     pages and the related-products rails all 500.
 *   • homeSections() / homeSliderBanners() / activeCategories() / the coupon,
 *     shipping-zone and loyalty lookups in checkout → silently empty.
 *   • The phone/email uniqueness checks in StorefrontAuthService → a customer
 *     could register a phone number that already exists.
 *
 * ASG Group runs ONE storefront for itself, not a subdomain-per-tenant SaaS,
 * so one configured default organisation is seeded rather than building real
 * host-based tenant resolution.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHAT CHANGED
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ── CHANGE 1 — path list no longer hand-duplicated. ★ FIXES A LIVE BUG ─────
 *   This filter's STOREFRONT_PATTERNS array carried "/about", "/contact",
 *   "/faq", "/page/**" and "/newsletter/**". SecurityConfig.PUBLIC_URLS did
 *   NOT. Two hand-maintained copies of the same security-relevant list, one
 *   of them carrying a comment claiming they were "kept in sync". They were
 *   not. Result: an anonymous visitor clicking About / Contact / FAQ in the
 *   storefront footer got bounced to /login?expired. Both consumers now import
 *   StorefrontPaths. One list. It cannot drift again.
 *
 * ── CHANGE 2 — no longer forces an HTTP session for every anonymous hit ────
 *   The old filter did `holder.get()` unconditionally. `holder` is a
 *   session-scoped bean behind a scoped proxy: resolving it CREATES an
 *   HttpSession if none exists. With spring.session.store-type=jdbc that is a
 *   row in SPRING_SESSION plus a row in SPRING_SESSION_ATTRIBUTES, held for 30
 *   minutes — for every crawler, every uptime probe, every drive-by bot that
 *   ever loads the home page.
 *
 *   Now the org is handed to the request through a request-scoped ThreadLocal
 *   (ContextProvider.setAnonymousOrganization), and the session-backed holder
 *   is only touched when a session ALREADY exists. Read paths for anonymous
 *   visitors create zero session rows.
 *
 *   ★ HONEST CAVEAT: this does not by itself make the storefront session-free.
 *     CSRF uses HttpSessionCsrfTokenRepository, so any page whose Thymeleaf
 *     template renders ${_csrf.token} (the <meta name="_csrf"> tag that
 *     secureFetch() reads) still creates a session on render. What this change
 *     buys is that ORG RESOLUTION is no longer the thing forcing it, and that
 *     the storefront now works correctly even on a request that has no session
 *     at all — which is the precondition for ever moving CSRF to a cookie
 *     repository and getting genuinely session-free anonymous browsing.
 *
 * ── CHANGE 3 — ThreadLocal is always cleared in a finally block ────────────
 *   Request threads are pooled. A ThreadLocal left set on a Tomcat worker
 *   would leak one visitor's org context into the NEXT unrelated request that
 *   the same thread happens to serve. try/finally, no exceptions.
 *
 * ── CHANGE 4 — explicit filter order ──────────────────────────────────────
 *   Previously implicit (LOWEST_PRECEDENCE). It happened to work, because
 *   Spring Security's chain registers at order -100 and calls chain.doFilter()
 *   down to the rest of the servlet filters, so this filter still ran before
 *   the DispatcherServlet. But "happened to work by default" is not a
 *   guarantee; the order is now stated.
 *
 * ── Configuration ─────────────────────────────────────────────────────────
 *   app.storefront.default-organization-id=<numeric id of the ASG org>
 *
 * ── Upgrading to real subdomain multi-tenancy later ────────────────────────
 *   Replace resolveDefaultOrg() with a Host-header → Organization lookup.
 *   Nothing else changes: every storefront service reads the org through
 *   ContextProvider, never through this filter directly.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class StorefrontOrgContextFilter extends OncePerRequestFilter {

    private final UserContextHolder holder;
    private final OrganizationRepository organizationRepository;

    @Value("${app.storefront.default-organization-id:#{null}}")
    private Long defaultOrganizationId;

    /**
     * Cached after first resolution — one DB hit for the whole app lifetime
     * instead of one per anonymous pageview. Safe to cache: the configured id
     * only changes on restart.
     */
    private volatile Organization cachedDefaultOrg;

    /** Log the "not configured" warning once, not once per request. */
    private volatile boolean warnedMissingConfig = false;

    public StorefrontOrgContextFilter(UserContextHolder holder,
                                      OrganizationRepository organizationRepository) {
        this.holder = holder;
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!StorefrontPaths.needsOrgContext(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        boolean seeded = false;
        try {
            seeded = seedOrgContext(request);
            chain.doFilter(request, response);
        } finally {
            // ✅ CHANGE 3 — request threads are pooled. Always clear.
            if (seeded) ContextProvider.clearAnonymousOrganization();
        }
    }

    /**
     * @return true if a request-scoped anonymous org was set (and therefore must
     *         be cleared in the caller's finally block).
     */
    private boolean seedOrgContext(HttpServletRequest request) {

        // ── 1. Does this request already carry a REAL ERP staff context? ─────
        //    Only look inside the session if one already exists — reading the
        //    session-scoped holder is what would otherwise create it.
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            UserContextDTO ctx = holder.get();          // safe: session exists
            if (ctx.getOrganizationId() != null) {
                // A logged-in ERP staff member (e.g. an org-admin previewing the
                // storefront). NEVER overwrite their real org context.
                return false;
            }
        }

        // ── 2. Resolve the configured default organisation ───────────────────
        if (defaultOrganizationId == null) {
            if (!warnedMissingConfig) {
                warnedMissingConfig = true;
                log.warn("Anonymous storefront request to '{}' but app.storefront.default-organization-id " +
                         "is not configured. ContextProvider.getOrganizationId() will return null and every " +
                         "storefront query will find nothing — or throw, since browse() interpolates orgId " +
                         "straight into the SQL text. Set it to the numeric id of the storefront's org: " +
                         "SELECT id, code, name FROM org_organizations ORDER BY id;",
                         request.getRequestURI());
            }
            return false;
        }

        Organization org = resolveDefaultOrg();
        if (org == null) {
            log.warn("app.storefront.default-organization-id={} matches no row in org_organizations.",
                     defaultOrganizationId);
            return false;
        }

        // ── 3. Hand the org to THIS request without creating a session ───────
        ContextProvider.setAnonymousOrganization(org.getId(), org.getName());

        // ── 4. If a session already exists, seed the holder too, so that the
        //       org survives into requests where this filter does not run
        //       (e.g. an admin endpoint the shopper is not supposed to reach
        //       anyway, but also any future storefront path not yet listed).
        //       Note this NEVER creates a session — guarded above.
        if (existingSession != null) {
            UserContextDTO ctx = holder.get();
            if (ctx.getOrganizationId() == null) {
                ctx.setOrganizationId(org.getId());
                ctx.setOrganizationName(org.getName());
                holder.set(ctx);
            }
        }

        return true;
    }

    private Organization resolveDefaultOrg() {
        Organization cached = cachedDefaultOrg;
        if (cached != null && defaultOrganizationId.equals(cached.getId())) return cached;
        Organization org = organizationRepository.findById(defaultOrganizationId).orElse(null);
        cachedDefaultOrg = org;
        return org;
    }
}
