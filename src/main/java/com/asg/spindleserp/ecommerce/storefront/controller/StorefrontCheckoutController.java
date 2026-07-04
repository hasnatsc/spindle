// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontCheckoutController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCheckoutDTO;
import com.asg.spindleserp.ecommerce.storefront.service.*;
import com.asg.spindleserp.security.auth.ContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontCheckoutController — checkout page, order placement, success page.
 * Pages: /checkout   /checkout/success/{orderNo}
 * REST:  POST /checkout/place-order
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class StorefrontCheckoutController {

    private final StorefrontCheckoutService checkoutService;
    private final StorefrontCartService cartService;
    private final StorefrontAddressService addressService;
    private final StorefrontAuthService authService;
    private final EcOrderRepository orderRepository;

    @GetMapping
    public String checkoutPage(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/checkout";

        var cart = cartService.viewCart(request, customer);
        if (cart.getItems() == null || cart.getItems().isEmpty()) return "redirect:/cart";

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addressService.list(customer.getId()));
        model.addAttribute("appliedCoupon",
                checkoutService.appliedCoupon(request, customer,
                        cart.getSubtotal() != null ? cart.getSubtotal() : BigDecimal.ZERO));
        return "ecommerce/storefront/sf-checkout";
    }

    @PostMapping("/place-order")
    @ResponseBody
    public Map<String, Object> placeOrder(@RequestBody SfCheckoutDTO dto, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            String orderNo = checkoutService.placeOrder(request, customer, dto);
            res.put("success", true);
            res.put("orderNo", orderNo);
            res.put("message", "Order placed successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/success/{orderNo}")
    public String success(@PathVariable String orderNo, Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login";

        var order = orderRepository.findByOrganizationIdAndOrderNo(
                ContextProvider.getOrganizationId(), orderNo).orElse(null);
        if (order == null || order.getCustomer() == null
                || !order.getCustomer().getId().equals(customer.getId()))
            return "redirect:/account/orders";

        model.addAttribute("customer", customer);
        model.addAttribute("order", order);
        return "ecommerce/storefront/sf-order-success";
    }
}
