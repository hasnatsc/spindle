// Path: com/asg/spindleserp/storefront/controller/StorefrontCartController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontCartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontCartController — cart drawer/page for both guests and customers.
 * Page: /cart
 * REST: /cart/view  /cart/add  /cart/update/{itemId}  /cart/remove/{itemId}  /cart/count
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class StorefrontCartController {

    private final StorefrontCartService cartService;
    private final StorefrontAuthService authService;

    @GetMapping
    public String cartPage(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        model.addAttribute("cart", cartService.viewCart(request, customer));
        return "ecommerce/storefront/sf-cart";
    }

    @GetMapping("/view")
    @ResponseBody
    public Map<String, Object> view(HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            res.put("success", true);
            res.put("cart", cartService.viewCart(request, customer));
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            Long productId = Long.valueOf(body.get("productId").toString());
            Long variantId = body.get("variantId") != null ? Long.valueOf(body.get("variantId").toString()) : null;
            BigDecimal qty = new BigDecimal(body.getOrDefault("quantity", "1").toString());
            var cart = cartService.addItem(request, customer, productId, variantId, qty);
            res.put("success", true);
            res.put("message", "Added to cart.");
            res.put("cart", cart);
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/update/{itemId}")
    @ResponseBody
    public Map<String, Object> update(@PathVariable Long itemId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            BigDecimal qty = new BigDecimal(body.getOrDefault("quantity", "0").toString());
            var cart = cartService.updateQuantity(request, customer, itemId, qty);
            res.put("success", true); res.put("cart", cart);
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/remove/{itemId}")
    @ResponseBody
    public Map<String, Object> remove(@PathVariable Long itemId, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            var cart = cartService.removeItem(request, customer, itemId);
            res.put("success", true); res.put("message", "Removed from cart."); res.put("cart", cart);
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/count")
    @ResponseBody
    public Map<String, Object> count(HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        return Map.of("count", cartService.cartItemCount(request, customer));
    }
}
