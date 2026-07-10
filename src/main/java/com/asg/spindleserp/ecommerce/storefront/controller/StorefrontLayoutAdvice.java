// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontLayoutAdvice.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.storefront.service.StorefrontProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * StorefrontLayoutAdvice — populates the theme shell (sf-base.html) on every
 * storefront page render:
 *   navCategories  → "Shop By Categories" mega-menu sidebar
 *   footerProducts → footer "LATEST PRODUCT" mini list (3 newest)
 *
 * Scoped to this package's controllers only, so ERP/admin controllers are
 * untouched. Skips REST (@ResponseBody) rendering automatically because
 * @ModelAttribute values are only consumed by view rendering.
 *
 * ★ Perf seam — this runs two small queries per page view. Both are cheap
 *   (indexed, LIMIT 3 / active-only), but if storefront traffic grows put a
 *   short TTL cache (Caffeine @Cacheable, 60s) on activeCategories() and
 *   newArrivals(3).
 */
@ControllerAdvice(basePackages = "com.asg.spindleserp.ecommerce.storefront.controller")
@RequiredArgsConstructor
public class StorefrontLayoutAdvice {

    private final StorefrontProductService productService;

    @ModelAttribute
    public void layoutModel(Model model) {
        try {
            model.addAttribute("navCategories", productService.activeCategories());
            model.addAttribute("footerProducts", productService.newArrivals(3));
        } catch (Exception ignored) {
            // anonymous org context missing etc. — shell renders its fallbacks
        }
    }
}
