// Path: com/asg/spindleserp/storefront/controller/StorefrontCatalogController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.storefront.service.StorefrontProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * StorefrontCatalogController — public product browsing.
 * Pages: /  /shop  /product/{slug}  /category/{slug}
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/storefront")
public class StorefrontCatalogController {

    private final StorefrontProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featured", productService.featured(8));
        model.addAttribute("newArrivals", productService.newArrivals(8));
        model.addAttribute("categories", productService.activeCategories());
        return "ecommerce/storefront/sf-home";
    }

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) Long category,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String sort,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        Map<String, Object> result = productService.browse(category, q, sort, page, 24);
        model.addAttribute("result", result);
        model.addAttribute("categories", productService.activeCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("query", q);
        model.addAttribute("sort", sort);
        return "ecommerce/storefront/sf-shop";
    }

    @GetMapping("/category/{slug}")
    public String categoryBySlug(@PathVariable String slug,
                                  @RequestParam(defaultValue = "1") int page,
                                  Model model) {
        var categories = productService.activeCategories();
        var match = categories.stream().filter(c -> slug.equals(c.getSlug())).findFirst();
        Long catId = match.map(c -> c.getId()).orElse(null);

        Map<String, Object> result = productService.browse(catId, null, "featured", page, 24);
        model.addAttribute("result", result);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategory", match.orElse(null));
        return "ecommerce/storefront/sf-shop";
    }

    @GetMapping("/product/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        model.addAttribute("product", productService.findBySlug(slug));
        return "ecommerce/storefront/sf-product-detail";
    }
}
