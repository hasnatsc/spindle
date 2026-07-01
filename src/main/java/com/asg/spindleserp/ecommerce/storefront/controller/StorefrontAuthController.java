// Path: com/asg/spindleserp/storefront/controller/StorefrontAuthController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAuthDTO;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontCartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontAuthController — customer registration / login / logout.
 * Pages:  /account/register  /account/login
 * REST:   /account/register  /account/login  /account/logout
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class StorefrontAuthController {

    private final StorefrontAuthService authService;
    private final StorefrontCartService cartService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, Model model, HttpServletRequest request) {
        if (authService.isLoggedIn(request)) return "redirect:/account/dashboard";
        model.addAttribute("redirectUrl", redirect != null ? redirect : "/account/dashboard");
        return "ecommerce/storefront/sf-login";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String redirect, Model model, HttpServletRequest request) {
        if (authService.isLoggedIn(request)) return "redirect:/account/dashboard";
        model.addAttribute("redirectUrl", redirect != null ? redirect : "/account/dashboard");
        return "ecommerce/storefront/sf-register";
    }

    @PostMapping("/register")
    @ResponseBody
    public Map<String, Object> register(@RequestBody SfAuthDTO dto, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            SfAuthDTO created = authService.register(dto, request);
            EcCustomer customer = authService.currentCustomerOrNull(request);
            if (customer != null) cartService.mergeGuestCartOnLogin(request, customer);
            res.put("success", true);
            res.put("message", "Welcome, " + created.getFirstName() + "! Your account is ready.");
            res.put("customer", created);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/login")
    @ResponseBody
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            SfAuthDTO customer = authService.login(body.get("identifier"), body.get("password"), request);
            EcCustomer c = authService.currentCustomerOrNull(request);
            if (c != null) cartService.mergeGuestCartOnLogin(request, c);
            res.put("success", true);
            res.put("message", "Welcome back, " + customer.getFirstName() + "!");
            res.put("customer", customer);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        authService.logout(request);
        return "redirect:/";
    }
}
