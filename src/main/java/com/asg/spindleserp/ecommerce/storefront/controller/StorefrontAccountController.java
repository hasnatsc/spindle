// Path: com/asg/spindleserp/storefront/controller/StorefrontAccountController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StorefrontAccountController — the logged-in customer portal.
 * Pages: /account/dashboard  /account/orders  /account/orders/{orderNo}  /account/profile
 *
 * Every method first resolves the current customer via StorefrontAuthService;
 * if absent, redirects to /account/login?redirect=<original path>.
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class StorefrontAccountController {

    private final StorefrontAuthService authService;
    private final EcOrderRepository orderRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        EcCustomer customer = requireCustomerOrRedirect(model, request, "/account/dashboard");
        if (customer == null) return "redirect:/account/login?redirect=/account/dashboard";

        List<EcOrder> recentOrders = orderRepository.findByCustomerIdOrderByIdDesc(customer.getId())
                .stream().limit(5).toList();

        model.addAttribute("customer", customer);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("totalOrders", customer.getTotalOrders());
        model.addAttribute("totalPurchase", customer.getTotalPurchase());
        model.addAttribute("rewardPoints", customer.getRewardPoints());
        return "ecommerce/storefront/sf-account-dashboard";
    }

    @GetMapping("/orders")
    public String orders(Model model, HttpServletRequest request) {
        EcCustomer customer = requireCustomerOrRedirect(model, request, "/account/orders");
        if (customer == null) return "redirect:/account/login?redirect=/account/orders";

        model.addAttribute("customer", customer);
        model.addAttribute("orders", orderRepository.findByCustomerIdOrderByIdDesc(customer.getId()));
        return "storefront/sf-account-orders";
    }

    @GetMapping("/orders/{orderNo}")
    public String orderDetail(@PathVariable String orderNo, Model model, HttpServletRequest request) {
        EcCustomer customer = requireCustomerOrRedirect(model, request, "/account/orders/" + orderNo);
        if (customer == null) return "redirect:/account/login?redirect=/account/orders/" + orderNo;

        EcOrder order = orderRepository.findByOrganizationIdAndOrderNo(
                com.asg.spindleserp.security.auth.ContextProvider.getOrganizationId(), orderNo)
                .orElse(null);
        if (order == null || order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            model.addAttribute("notFound", true);
            return "storefront/sf-account-order-detail";
        }
        model.addAttribute("customer", customer);
        model.addAttribute("order", order);
        return "storefront/sf-account-order-detail";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        EcCustomer customer = requireCustomerOrRedirect(model, request, "/account/profile");
        if (customer == null) return "redirect:/account/login?redirect=/account/profile";
        model.addAttribute("customer", customer);
        return "storefront/sf-account-profile";
    }

    @PostMapping("/profile")
    @ResponseBody
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) { res.put("success", false); res.put("message", "Please log in."); return res; }
        try {
            if (body.get("firstName") != null && !body.get("firstName").isBlank())
                customer.setFirstName(body.get("firstName").trim());
            if (body.get("lastName") != null) customer.setLastName(body.get("lastName").trim());
            customer.setFullName((customer.getFirstName() + " " + (customer.getLastName() != null ? customer.getLastName() : "")).trim());
            if (body.get("email") != null && !body.get("email").isBlank())
                customer.setEmail(body.get("email").trim().toLowerCase());
            res.put("success", true);
            res.put("message", "Profile updated.");
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    // ── HELPER ───────────────────────────────────────────────────────────────
    private EcCustomer requireCustomerOrRedirect(Model model, HttpServletRequest request, String path) {
        return authService.currentCustomerOrNull(request);
    }
}
