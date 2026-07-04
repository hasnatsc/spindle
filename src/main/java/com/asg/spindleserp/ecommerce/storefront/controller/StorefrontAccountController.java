// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontAccountController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAddressDTO;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAddressService;
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
 * Pages: /account/dashboard  /account/orders  /account/orders/{orderNo}
 *        /account/profile  /account/addresses
 *
 * v2 changes:
 *  - Fixed template prefixes ("storefront/…" → "ecommerce/storefront/…").
 *  - Profile POST now persists via authService.updateProfile() (the old code
 *    mutated a detached entity and silently saved nothing).
 *  - New address book page + AJAX CRUD (save / delete / set-default).
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class StorefrontAccountController {

    private final StorefrontAuthService authService;
    private final StorefrontAddressService addressService;
    private final EcOrderRepository orderRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
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
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/account/orders";

        model.addAttribute("customer", customer);
        model.addAttribute("orders", orderRepository.findByCustomerIdOrderByIdDesc(customer.getId()));
        return "ecommerce/storefront/sf-account-orders";
    }

    @GetMapping("/orders/{orderNo}")
    public String orderDetail(@PathVariable String orderNo, Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/account/orders/" + orderNo;

        EcOrder order = orderRepository.findByOrganizationIdAndOrderNo(
                com.asg.spindleserp.security.auth.ContextProvider.getOrganizationId(), orderNo)
                .orElse(null);
        if (order == null || order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            model.addAttribute("notFound", true);
            model.addAttribute("customer", customer);
            return "ecommerce/storefront/sf-account-order-detail";
        }
        model.addAttribute("customer", customer);
        model.addAttribute("order", order);
        return "ecommerce/storefront/sf-account-order-detail";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/account/profile";
        model.addAttribute("customer", customer);
        return "ecommerce/storefront/sf-account-profile";
    }

    @PostMapping("/profile")
    @ResponseBody
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) { res.put("success", false); res.put("message", "Please log in."); return res; }
        try {
            authService.updateProfile(customer.getId(),
                    body.get("firstName"), body.get("lastName"), body.get("email"));
            res.put("success", true);
            res.put("message", "Profile updated.");
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    // ── ADDRESS BOOK ─────────────────────────────────────────────────────────
    @GetMapping("/addresses")
    public String addresses(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/account/addresses";
        model.addAttribute("customer", customer);
        model.addAttribute("addresses", addressService.list(customer.getId()));
        return "ecommerce/storefront/sf-account-addresses";
    }

    @PostMapping("/addresses")
    @ResponseBody
    public Map<String, Object> saveAddress(@RequestBody SfAddressDTO dto, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) { res.put("success", false); res.put("message", "Please log in."); return res; }
        try {
            Long id = addressService.save(customer.getId(), dto);
            res.put("success", true);
            res.put("message", "Address saved.");
            res.put("id", id);
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseBody
    public Map<String, Object> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) { res.put("success", false); res.put("message", "Please log in."); return res; }
        try {
            addressService.delete(customer.getId(), id);
            res.put("success", true);
            res.put("message", "Address removed.");
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/addresses/{id}/default")
    @ResponseBody
    public Map<String, Object> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) { res.put("success", false); res.put("message", "Please log in."); return res; }
        try {
            addressService.setDefault(customer.getId(), id);
            res.put("success", true);
            res.put("message", "Default address updated.");
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }
}
