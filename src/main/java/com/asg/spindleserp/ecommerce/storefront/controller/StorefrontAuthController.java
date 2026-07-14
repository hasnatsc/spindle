// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontAuthController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAuthDTO;
import com.asg.spindleserp.ecommerce.storefront.security.StorefrontAuthException;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontCartService;
import com.asg.spindleserp.security.auth.WebSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontAuthController — customer registration / login / logout.
 *
 * Pages:  GET  /account/login      GET  /account/register
 * REST:   POST /account/login      POST /account/register
 *         POST /account/logout     POST /account/change-password
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHAT WAS WRONG
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] 1. OPEN REDIRECT — a ready-made phishing primitive             ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   Both page handlers did this:
 *
 *       @GetMapping("/login")
 *       public String loginPage(@RequestParam(required = false) String redirect, Model model, …) {
 *           model.addAttribute("redirectUrl", redirect != null ? redirect : "/account/dashboard");
 *           return "ecommerce/storefront/sf-login";
 *       }
 *
 *   The raw query parameter goes straight into the model, and the login
 *   template hands it to the browser after a successful sign-in. So:
 *
 *       https://shop.asg.com/account/login?redirect=https://asg-shop.example/login
 *
 *   is a link on the REAL domain, serving the REAL page, under the REAL TLS
 *   certificate — which signs the customer in and then drops them onto a
 *   pixel-perfect clone that asks them to "confirm" their password. Every
 *   signal a careful user is taught to check (the domain, the padlock, the
 *   fact that the login actually worked) says this is legitimate.
 *
 *   Redirect parameters are unavoidable here — the checkout flow genuinely
 *   needs "sign in, then come back to where you were". The fix is not to
 *   remove the parameter but to constrain it to a same-site absolute PATH:
 *   WebSecurityUtils.safeRedirect() rejects absolute URLs, protocol-relative
 *   //host, backslash tricks, embedded schemes, and traversal. Anything that
 *   fails validation falls back to /account/dashboard instead of being
 *   reflected.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] 2. Internal exception messages echoed to the browser         ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *       } catch (Exception e) {
 *           res.put("success", false);
 *           res.put("message", e.getMessage());      // ← anything at all
 *       }
 *
 *   catch (Exception) + getMessage() means a Postgres unique-constraint
 *   violation renders in the shopper's browser as something like:
 *
 *     "could not execute statement [ERROR: duplicate key value violates unique
 *      constraint "uq_ec_customer_code" Detail: Key (organization_id,
 *      customer_code)=(1, CUST-482913) already exists.]"
 *
 *   — which is not hypothetical here, because the old customer-code generator
 *   was `"CUST-" + System.currentTimeMillis() % 1000000`, a value that wraps
 *   every ~16 minutes and collides on concurrent signups. So this WOULD fire,
 *   and it hands an attacker the table name, the column names, the constraint
 *   name and a live row's contents, for free.
 *
 *   Now: exceptions this codebase AUTHORED (StorefrontAuthException) carry
 *   messages that were written to be read by a customer, and are shown.
 *   Anything else is logged with a stack trace and shown as a generic message.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] 3. Throttling had no wire representation                     ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   A locked-out client now gets HTTP 429 + Retry-After + a JSON body carrying
 *   retryAfterSeconds, so the UI can show a real countdown instead of looping a
 *   generic "invalid password" forever.
 *
 * ── Also fixed ────────────────────────────────────────────────────────────
 *   • The "already logged in" check on GET /login and GET /register now
 *     honours the redirect target instead of always dumping the customer on
 *     /account/dashboard.
 *   • POST /account/logout now returns a redirect that cannot be poisoned.
 *   • POST /account/change-password added — customers previously had no way to
 *     change their own password at all.
 *
 * ── Unchanged, on purpose ─────────────────────────────────────────────────
 *   Response envelope shape ({success, message, customer, login}) is byte-for-
 *   byte what the existing storefront JavaScript already parses. The only
 *   additions are optional fields (redirectUrl, blocked, retryAfterSeconds)
 *   that old JS simply ignores. No frontend change is required to deploy this.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class StorefrontAuthController {

    private static final String DEFAULT_TARGET = "/account/dashboard";
    private static final String GENERIC_ERROR  = "Something went wrong. Please try again.";

    private final StorefrontAuthService authService;
    private final StorefrontCartService cartService;

    // ══════════════════════════════════════════════════════════════════════
    // PAGES
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect,
                            Model model, HttpServletRequest request) {
        String target = safeTarget(redirect);
        if (authService.isLoggedIn(request)) return "redirect:" + target;
        model.addAttribute("redirectUrl", target);          // ✅ validated, not raw
        return "ecommerce/storefront/sf-login";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String redirect,
                               Model model, HttpServletRequest request) {
        String target = safeTarget(redirect);
        if (authService.isLoggedIn(request)) return "redirect:" + target;
        model.addAttribute("redirectUrl", target);          // ✅ validated, not raw
        return "ecommerce/storefront/sf-register";
    }

    // ══════════════════════════════════════════════════════════════════════
    // REST — REGISTER
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/register")
    @ResponseBody
    public Map<String, Object> register(@RequestBody SfAuthDTO dto,
                                        @RequestParam(required = false) String redirect,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        Map<String, Object> res = new HashMap<>();
        try {
            SfAuthDTO created = authService.register(dto, request);

            EcCustomer customer = authService.currentCustomerOrNull(request);
            if (customer != null) cartService.mergeGuestCartOnLogin(request, customer);

            res.put("success", true);
            res.put("message", "Welcome, " + created.getFirstName() + "! Your account is ready.");
            res.put("customer", created);
            res.put("redirectUrl", safeTarget(redirect));

        } catch (StorefrontAuthException.TooManyAttempts e) {
            tooManyRequests(res, response, e);
        } catch (StorefrontAuthException e) {
            fail(res, e.getMessage());
        } catch (Exception e) {
            unexpected(res, "register", e);
        }
        return res;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REST — LOGIN
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/login")
    @ResponseBody
    public Map<String, Object> login(@RequestBody Map<String, String> body,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Map<String, Object> res = new HashMap<>();
        try {
            SfAuthDTO customer = authService.login(
                    body.get("identifier"), body.get("password"), request);

            EcCustomer c = authService.currentCustomerOrNull(request);
            if (c != null) cartService.mergeGuestCartOnLogin(request, c);

            res.put("success", true);
            res.put("message", "Welcome back, " + customer.getFirstName() + "!");
            res.put("customer", customer);
            // The redirect target is submitted in the JSON body by the login form
            // (it was rendered into the page from the validated ?redirect= param),
            // and is re-validated here rather than trusted a second time.
            res.put("redirectUrl", safeTarget(body.get("redirect")));

        } catch (StorefrontAuthException.TooManyAttempts e) {
            tooManyRequests(res, response, e);
        } catch (StorefrontAuthException e) {
            fail(res, e.getMessage());
        } catch (Exception e) {
            unexpected(res, "login", e);
        }
        return res;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REST — CHANGE PASSWORD  (new)
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body,
                                              HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();

        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) {
            res.put("success", false);
            res.put("login", true);
            res.put("message", "Please sign in to continue.");
            return res;
        }

        try {
            authService.changePassword(customer.getId(),
                    body.get("currentPassword"), body.get("newPassword"), request);
            res.put("success", true);
            res.put("message", "Your password has been updated.");
        } catch (StorefrontAuthException e) {
            fail(res, e.getMessage());
        } catch (Exception e) {
            unexpected(res, "changePassword", e);
        }
        return res;
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * POST only — a GET logout is CSRF-able (an <img src="/account/logout"> on
     * any page logs the visitor out). It was already POST; keeping it POST is
     * deliberate, not incidental, and CSRF protection applies because
     * /account/** is permitAll but NOT csrf-ignored.
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        authService.logout(request);
        return "redirect:/";
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** ✅ THE OPEN-REDIRECT FIX. Same-site absolute paths only. */
    private static String safeTarget(String candidate) {
        return WebSecurityUtils.safeRedirect(candidate, DEFAULT_TARGET);
    }

    private static void fail(Map<String, Object> res, String safeMessage) {
        res.put("success", false);
        res.put("message", safeMessage);
    }

    private static void tooManyRequests(Map<String, Object> res,
                                        HttpServletResponse response,
                                        StorefrontAuthException.TooManyAttempts e) {
        response.setStatus(429);                                    // Too Many Requests
        response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        res.put("success", false);
        res.put("blocked", true);
        res.put("retryAfterSeconds", e.getRetryAfterSeconds());
        res.put("message", e.getMessage());
    }

    /**
     * The one thing this must never do is put e.getMessage() on the wire.
     * Log it — with the stack trace, because an unexpected exception here is a
     * bug that someone needs to see — and tell the customer nothing.
     */
    private static void unexpected(Map<String, Object> res, String operation, Exception e) {
        log.error("Storefront auth '{}' failed unexpectedly", operation, e);
        res.put("success", false);
        res.put("message", GENERIC_ERROR);
    }
}
