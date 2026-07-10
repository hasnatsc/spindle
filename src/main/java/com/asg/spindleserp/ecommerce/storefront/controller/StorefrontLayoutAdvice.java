// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontLayoutAdvice.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.storefront.service.StorefrontProductService;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * StorefrontLayoutAdvice — populates the theme shell (sf-base.html) on every
 * storefront page render:
 *   navCategories  → "Shop By Categories" mega-menu sidebar
 *   footerProducts → footer "LATEST PRODUCT" mini list (3 newest)
 *
 * Scoped to this package's controllers only — ERP/admin controllers untouched.
 *
 * v3 OPTIMIZATION — 60-second in-memory TTL cache, keyed by orgId, so the two
 * shell queries run at most once a minute per org instead of once per page
 * view. No cache library needed: a volatile snapshot + timestamp is enough
 * because stale-for-up-to-60s nav data is harmless, and last-write-wins under
 * concurrency is fine (both writers computed the same thing).
 */
@ControllerAdvice(basePackages = "com.asg.spindleserp.ecommerce.storefront.controller")
@RequiredArgsConstructor
public class StorefrontLayoutAdvice {

    private static final long TTL_MS = 60_000;

    private final StorefrontProductService productService;

    private record Shell(long at, Long orgId, List<?> categories, List<?> footerProducts) {}
    private volatile Shell cache;

    @ModelAttribute
    public void layoutModel(Model model) {
        try {
            Long orgId = ContextProvider.getOrganizationId();
            if (orgId == null) return; // shell renders its static fallbacks

            Shell snap = cache;
            if (snap == null || !orgId.equals(snap.orgId())
                    || System.currentTimeMillis() - snap.at() > TTL_MS) {
                snap = new Shell(System.currentTimeMillis(), orgId,
                        productService.activeCategories(),
                        productService.newArrivals(3));
                cache = snap;
            }
            model.addAttribute("navCategories", snap.categories());
            model.addAttribute("footerProducts", snap.footerProducts());
        } catch (Exception ignored) {
            // never let shell decoration break a page render
        }
    }
}
