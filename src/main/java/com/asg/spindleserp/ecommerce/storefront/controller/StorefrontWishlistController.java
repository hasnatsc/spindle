// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontWishlistController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontProductService;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontWishlistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontWishlistController.
 * Page: /account/wishlist
 * REST: POST /wishlist/toggle {productId} → {success, inWishlist} — or
 *       {success:false, login:true} for anonymous visitors (JS redirects to login).
 */
@Controller
@RequiredArgsConstructor
public class StorefrontWishlistController {

    private final StorefrontWishlistService wishlistService;
    private final StorefrontProductService productService;
    private final StorefrontAuthService authService;

    @GetMapping("/account/wishlist")
    public String wishlistPage(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/account/wishlist";
        model.addAttribute("customer", customer);
        model.addAttribute("items", productService.cardsForWishlist(customer.getId()));
        model.addAttribute("wishlistIds", wishlistService.productIds(customer.getId()));
        return "ecommerce/storefront/sf-account-wishlist";
    }

    @PostMapping("/wishlist/toggle")
    @ResponseBody
    public Map<String, Object> toggle(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) {
            res.put("success", false);
            res.put("login", true);
            res.put("message", "Please sign in to save items to your wishlist.");
            return res;
        }
        try {
            Long productId = Long.valueOf(body.get("productId").toString());
            boolean inWishlist = wishlistService.toggle(customer.getId(), productId);
            res.put("success", true);
            res.put("inWishlist", inWishlist);
            res.put("message", inWishlist ? "Added to your wishlist." : "Removed from your wishlist.");
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }
}
