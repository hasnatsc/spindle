// Path: com/asg/spindleserp/ecommerce/storefront/config/StorefrontOrgContextFilter.java
package com.asg.spindleserp.ecommerce.storefront;

import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.dto.UserContextDTO;
import com.asg.spindleserp.security.session.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * StorefrontOrgContextFilter
 *
 * ══════════════════════════════════════════════════════════════════════
 * WHY THIS EXISTS
 * ══════════════════════════════════════════════════════════════════════
 * ContextProvider.getOrganizationId() reads Long organizationId from the
 * session-scoped UserContextHolder, which is ONLY EVER populated by
 * UserContextService.loadContext() — and that only runs on ERP staff
 * login (LoginSuccessHandler). An anonymous storefront/travel-site
 * visitor never goes through /login, so for the entire visit that
 * holder stays a brand-new, all-null UserContextDTO and
 * ContextProvider.getOrganizationId() returns null.
 *
 * That breaks more than the home page:
 *   - StorefrontProductService.browse() string-concatenates orgId
 *     straight into the SQL text ("WHERE p.organization_id = " + orgId).
 *     With orgId == null that becomes literal "= null" → a SQL syntax
 *     exception, not just an empty result. Shop, category pages, and
 *     the "continue shopping" related/recently-viewed rails all break
 *     the same way.
 *   - StorefrontContentService.homeSections()/homeSliderBanners(),
 *     StorefrontProductService.activeCategories(), the coupon /
 *     shipping-zone / loyalty-program lookups in StorefrontCheckoutService,
 *     and the phone/email uniqueness checks in StorefrontAuthService all
 *     silently query with orgId = NULL → empty results or broken
 *     constraints for every anonymous customer.
 *
 * ASG Group runs ONE storefront for itself — not a subdomain-per-tenant
 * SaaS — so instead of building real host/subdomain tenant resolution
 * (the ★ open seam TravelPortalController's and StorefrontCatalogController's
 * own comments already flag), this filter seeds ONE configured default
 * organization into the session's UserContextDTO the first time an
 * anonymous visitor hits a public storefront/travel-site URL.
 *
 * It NEVER overwrites an org already present in the session — if the
 * same browser session belongs to a logged-in ERP staff member (e.g. an
 * org-admin previewing the storefront), their real org context is left
 * untouched. It also never touches Spring Security's Authentication —
 * this only seeds the same session-scoped UserContextHolder that
 * ContextProvider already reads from everywhere else in the app, so
 * every existing ContextProvider.getOrganizationId() call downstream
 * just starts working, with zero changes to storefront services.
 *
 * ── Configuration (application.properties) ─────────────────────────────
 *   app.storefront.default-organization-id=<the org's numeric id>
 *
 * ── Upgrading to real multi-tenant subdomain resolution later ─────────
 *   Replace resolveDefaultOrg() below with a host/subdomain → Organization
 *   lookup. Nothing else needs to change — every storefront/travel-site
 *   service reads the org through ContextProvider, not through this filter
 *   directly.
 */
@Slf4j
@Component
public class StorefrontOrgContextFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Kept in sync with SecurityConfig.PUBLIC_URLS' storefront + travel-site entries. */
    private static final String[] STOREFRONT_PATTERNS = {
            "/", "/shop", "/shop/**", "/product/**", "/category/**",
            "/cart", "/cart/**", "/checkout", "/checkout/**",
            "/account", "/account/**", "/wishlist", "/wishlist/**",
            "/page/**", "/newsletter/**",
            "/travel-site", "/travel-site/**"
    };

    private final UserContextHolder holder;
    private final OrganizationRepository organizationRepository;

    @Value("${app.storefront.default-organization-id:#{null}}")
    private Long defaultOrganizationId;

    // Cached after first resolution — avoids a DB hit on every anonymous
    // pageview. Safe to cache: the configured id only changes on restart.
    private volatile Organization cachedDefaultOrg;

    public StorefrontOrgContextFilter(UserContextHolder holder,
                                      OrganizationRepository organizationRepository) {
        this.holder = holder;
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!isStorefrontPath(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        UserContextDTO ctx = holder.get();

        if (ctx.getOrganizationId() == null) {
            if (defaultOrganizationId == null) {
                log.warn("Anonymous storefront request to '{}' but app.storefront.default-organization-id " +
                        "is not configured — ContextProvider.getOrganizationId() will return null and " +
                        "storefront queries will find nothing (or throw on browse()).", request.getRequestURI());
            } else {
                Organization org = resolveDefaultOrg();
                if (org != null) {
                    ctx.setOrganizationId(org.getId());
                    ctx.setOrganizationName(org.getName());
                    holder.set(ctx);
                } else {
                    log.warn("app.storefront.default-organization-id={} does not match any organization row.",
                            defaultOrganizationId);
                }
            }
        }

        chain.doFilter(request, response);
    }

    private Organization resolveDefaultOrg() {
        Organization cached = cachedDefaultOrg;
        if (cached != null && cached.getId().equals(defaultOrganizationId)) return cached;
        Organization org = organizationRepository.findById(defaultOrganizationId).orElse(null);
        cachedDefaultOrg = org;
        return org;
    }

    private boolean isStorefrontPath(String uri) {
        for (String pattern : STOREFRONT_PATTERNS) {
            if (MATCHER.match(pattern, uri)) return true;
        }
        return false;
    }
}
