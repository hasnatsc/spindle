// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontCheckoutController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAddressDTO;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCheckoutDTO;
import com.asg.spindleserp.ecommerce.storefront.dto.SfCheckoutInfoDTO;
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
 * StorefrontCheckoutController v3 — Color Admin theme multi-step wizard.
 *
 * The theme's checkout is a 4-step flow (cart → info → payment → complete),
 * so the old single-page /checkout is replaced by:
 *
 *   GET  /checkout                → redirect:/checkout/info (back-compat)
 *   GET  /checkout/info           step 2 — shipping address page
 *   POST /checkout/info           validate + store SfCheckoutInfoDTO in session
 *   GET  /checkout/payment        step 3 — payment method + order summary
 *   POST /checkout/place-order    merge session info + {paymentMethod,customerNote}
 *                                 into SfCheckoutDTO → checkoutService.placeOrder()
 *                                 (the v2 service — UNCHANGED) → clears session key
 *   GET  /checkout/success/{no}   step 4 — org+customer-scoped, unchanged logic
 *
 * DELETE templates/ecommerce/storefront/sf-checkout.html — replaced by
 * sf-checkout-info.html + sf-checkout-payment.html.
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class StorefrontCheckoutController {

    public static final String SESSION_KEY_INFO = "SF_CHECKOUT_INFO";

    private final StorefrontCheckoutService checkoutService;
    private final StorefrontCartService cartService;
    private final StorefrontAddressService addressService;
    private final StorefrontAuthService authService;
    private final EcOrderRepository orderRepository;

    // ── STEP 1 → 2 bridge (old bookmarks / cart dropdown) ────────────────────
    @GetMapping
    public String checkoutRoot() {
        return "redirect:/checkout/info";
    }

    // ── STEP 2 — SHIPPING INFO ───────────────────────────────────────────────
    @GetMapping("/info")
    public String infoPage(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/checkout/info";

        var cart = cartService.viewCart(request, customer);
        if (cart.getItems() == null || cart.getItems().isEmpty()) return "redirect:/cart";

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addressService.list(customer.getId()));
        return "ecommerce/storefront/sf-checkout-info";
    }

    @PostMapping("/info")
    @ResponseBody
    public Map<String, Object> saveInfo(@RequestBody SfCheckoutInfoDTO dto, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            if (customer == null) { res.put("success", false); res.put("login", true); return res; }

            if (dto.getAddressId() != null) {
                // must belong to this customer — byId is customer-scoped and throws otherwise
                SfAddressDTO saved = addressService.byId(customer.getId(), dto.getAddressId());
                if (saved == null) throw new IllegalArgumentException("Address not found.");
            } else {
                if (isBlank(dto.getFullName()) || isBlank(dto.getPhone())
                        || isBlank(dto.getAddressLine1()) || isBlank(dto.getDistrict()))
                    throw new IllegalArgumentException("Name, phone, street address and district are required.");
            }

            request.getSession(true).setAttribute(SESSION_KEY_INFO, dto);
            res.put("success", true);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── STEP 3 — PAYMENT ─────────────────────────────────────────────────────
    @GetMapping("/payment")
    public String paymentPage(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/checkout/info";

        var cart = cartService.viewCart(request, customer);
        if (cart.getItems() == null || cart.getItems().isEmpty()) return "redirect:/cart";

        SfCheckoutInfoDTO info = (SfCheckoutInfoDTO)
                (request.getSession(false) != null
                        ? request.getSession(false).getAttribute(SESSION_KEY_INFO) : null);
        if (info == null) return "redirect:/checkout/info";

        // "Deliver to" block for the page
        Map<String, String> shipTo = new HashMap<>();
        if (info.getAddressId() != null) {
            SfAddressDTO a = addressService.byId(customer.getId(), info.getAddressId());
            if (a == null) return "redirect:/checkout/info";
            shipTo.put("fullName", a.getContactPerson());
            shipTo.put("phone", a.getContactPhone());
            shipTo.put("addressText", joinAddress(a));
        } else {
            shipTo.put("fullName", info.getFullName());
            shipTo.put("phone", info.getPhone());
            shipTo.put("addressText", info.toAddressText());
        }

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        model.addAttribute("shipTo", shipTo);
        model.addAttribute("appliedCoupon",
                checkoutService.appliedCoupon(request, customer,
                        cart.getSubtotal() != null ? cart.getSubtotal() : BigDecimal.ZERO));
        return "ecommerce/storefront/sf-checkout-payment";
    }

    // ── PLACE ORDER ──────────────────────────────────────────────────────────
    @PostMapping("/place-order")
    @ResponseBody
    public Map<String, Object> placeOrder(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcCustomer customer = authService.currentCustomerOrNull(request);
            if (customer == null) { res.put("success", false); res.put("login", true); return res; }

            SfCheckoutInfoDTO info = (SfCheckoutInfoDTO)
                    (request.getSession(false) != null
                            ? request.getSession(false).getAttribute(SESSION_KEY_INFO) : null);
            if (info == null) throw new IllegalStateException("Shipping info missing — please complete step 2.");

            SfCheckoutDTO dto = SfCheckoutDTO.builder()
                    .addressId(info.getAddressId())
                    .fullName(info.getFullName())
                    .phone(info.getPhone())
                    .addressLine1(info.getAddressLine1())
                    .addressLine2(info.getAddressLine2())
                    .area(info.getArea())
                    .upazila(info.getUpazila())
                    .district(info.getDistrict())
                    .division(info.getDivision())
                    .postCode(info.getPostCode())
                    .saveAddress(info.getSaveAddress())
                    .paymentMethod(body.getOrDefault("paymentMethod", "COD"))
                    .customerNote(body.get("customerNote"))
                    .build();

            String orderNo = checkoutService.placeOrder(request, customer, dto);
            request.getSession(false).removeAttribute(SESSION_KEY_INFO);

            res.put("success", true);
            res.put("orderNo", orderNo);
            res.put("message", "Order placed successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── STEP 4 — SUCCESS ─────────────────────────────────────────────────────
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

    // ── helpers ──────────────────────────────────────────────────────────────
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String joinAddress(SfAddressDTO a) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{a.getAddressLine1(), a.getAddressLine2(), a.getArea(),
                a.getUpazila(), a.getDistrict(), a.getDivision()}) {
            if (part == null || part.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(part.trim());
        }
        if (a.getPostCode() != null && !a.getPostCode().isBlank()) sb.append(" — ").append(a.getPostCode().trim());
        return sb.toString();
    }
}
