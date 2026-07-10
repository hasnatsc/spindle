// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontCatalogController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * StorefrontCatalogController — public product browsing.
 * Pages: /  /shop  /product/{slug}  /category/{slug}
 *
 * v2 changes:
 *  - Mapping moved from "/storefront" to the site root "/". ★ If the ERP already
 *    maps "/", either keep the old prefix or serve the storefront on its own
 *    subdomain/host — see README.
 *  - Home now renders banner slider + dynamic home sections; featured/newArrivals
 *    remain as the fallback when no sections are configured.
 *  - PDP records recently-viewed, loads reviews/summary/wishlist state and a
 *    "Recently viewed" strip; a dead slug now redirects to /shop instead of 500.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class StorefrontCatalogController {

    private final StorefrontProductService productService;
    private final StorefrontContentService contentService;
    private final StorefrontReviewService reviewService;
    private final StorefrontWishlistService wishlistService;
    private final StorefrontAuthService authService;

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);

        var banners  = contentService.homeSliderBanners();
        var sections = contentService.homeSections();

        model.addAttribute("banners", banners);
        model.addAttribute("homeSections", sections);
        if (sections.isEmpty()) {   // fallback shelves when no dynamic sections configured
            model.addAttribute("featured", productService.featured(8));
            model.addAttribute("newArrivals", productService.newArrivals(8));
        }
        model.addAttribute("categories", productService.activeCategories());
        model.addAttribute("activeNav", "home");
        model.addAttribute("wishlistIds", customer != null ? wishlistService.productIds(customer.getId()) : null);
        return "ecommerce/storefront/sf-home";
    }

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) Long category,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String sort,
                        @RequestParam(required = false) java.math.BigDecimal minPrice,
                        @RequestParam(required = false) java.math.BigDecimal maxPrice,
                        @RequestParam(defaultValue = "1") int page,
                        Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        Map<String, Object> result = productService.browse(category, q, sort, minPrice, maxPrice, page, 24);
        model.addAttribute("result", result);
        model.addAttribute("categories", productService.activeCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("query", q);
        model.addAttribute("sort", sort);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("activeNav", "shop");
        model.addAttribute("wishlistIds", customer != null ? wishlistService.productIds(customer.getId()) : null);
        return "ecommerce/storefront/sf-shop";
    }

    @GetMapping("/category/{slug}")
    public String categoryBySlug(@PathVariable String slug,
                                  @RequestParam(defaultValue = "1") int page,
                                  Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        var categories = productService.activeCategories();
        var match = categories.stream().filter(c -> slug.equals(c.getSlug())).findFirst();
        Long catId = match.map(c -> c.getId()).orElse(null);

        Map<String, Object> result = productService.browse(catId, null, "featured", null, null, page, 24);
        model.addAttribute("result", result);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategory", match.orElse(null));
        model.addAttribute("activeNav", "shop");
        model.addAttribute("wishlistIds", customer != null ? wishlistService.productIds(customer.getId()) : null);
        return "ecommerce/storefront/sf-shop";
    }

    @GetMapping("/product/{slug}")
    public String productDetail(@PathVariable String slug, Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);

        com.asg.spindleserp.ecommerce.storefront.dto.SfProductDetailDTO product;
        try {
            product = productService.findBySlug(slug);
        } catch (Exception e) {
            return "redirect:/shop";
        }

        if (customer != null)
            contentService.recordRecentlyViewed(customer.getId(), product.getId(), clientIp(request));

        model.addAttribute("product", product);
        model.addAttribute("customer", customer);
        model.addAttribute("reviews", reviewService.approved(product.getId(), 10));
        model.addAttribute("reviewSummary", reviewService.summary(product.getId()));
        model.addAttribute("inWishlist", customer != null && wishlistService.contains(customer.getId(), product.getId()));
        model.addAttribute("wishlistIds", customer != null ? wishlistService.productIds(customer.getId()) : null);
        model.addAttribute("recentlyViewed", customer != null
                ? productService.cardsRecentlyViewed(customer.getId(), 4, product.getId())
                : java.util.List.of());
        return "ecommerce/storefront/sf-product-detail";
    }

    private static String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
