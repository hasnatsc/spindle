// Path: com/asg/spindleserp/ecommerce/storefront/security/StorefrontAuthInterceptor.java
package com.asg.spindleserp.ecommerce.storefront.security;

import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.security.auth.WebSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * StorefrontAuthInterceptor — the ONE place a storefront customer login is
 * enforced.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * THE STRUCTURAL PROBLEM THIS SOLVES
 * ══════════════════════════════════════════════════════════════════════════
 * Because customers live outside Spring Security, EVERY customer-only endpoint
 * had to carry its own hand-written guard. There were twelve of them, spread
 * across four controllers, in three different shapes:
 *
 *   StorefrontAccountController      × 7   → "redirect:/account/login?redirect=…"
 *   StorefrontCheckoutController     × 4   → sometimes redirect, sometimes {login:true}
 *   StorefrontWishlistController     × 2   → sometimes redirect, sometimes {login:true}
 *   StorefrontReviewController       × 1   → {login:true}
 *
 * Every one of them was written correctly. That is not the point. The point is
 * that this is a security control implemented as a copy-paste convention, and
 * a convention is exactly one distracted afternoon away from a new
 * @GetMapping("/account/wallet") that forgets it — at which point any anonymous
 * visitor reads any customer's wallet by guessing an id, and nothing in the
 * codebase objects.
 *
 * Spring Security cannot help here: it cannot see SF_CUSTOMER_ID, so
 * .requestMatchers("/account/**").authenticated() would lock out every real
 * customer. The correct shape is a handler interceptor that runs before every
 * controller under the customer-only prefixes.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * BEHAVIOUR — deliberately identical to what the controllers already did
 * ══════════════════════════════════════════════════════════════════════════
 * The existing storefront JavaScript already knows how to react to both shapes,
 * so this interceptor reproduces them exactly and NOTHING in the frontend needs
 * to change:
 *
 *   AJAX / JSON request  → 401 + {"success":false,"login":true,"message":…}
 *                          (identical to what /wishlist/toggle and
 *                           /product/{id}/reviews already returned, so the
 *                           existing "if (r.login) location.href='/account/login'"
 *                           handlers keep working untouched)
 *
 *   Normal page request  → 302 → /account/login?redirect=<the page they wanted>
 *                          (identical to what the account/checkout pages already
 *                           did — and the redirect target is now passed through
 *                           WebSecurityUtils.safeRedirect + URL-encoded, closing
 *                           the open-redirect hole described in
 *                           StorefrontAuthController)
 *
 * The controllers KEEP their own checks. They are now unreachable-but-correct
 * belt and braces: if this interceptor is ever accidentally unregistered, the
 * app fails closed, not open.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorefrontAuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PAGE = "/account/login";

    private final StorefrontAuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (authService.currentCustomerOrNull(request) != null) {
            return true;   // signed in — carry on to the controller
        }

        String uri = request.getRequestURI();

        if (WebSecurityUtils.isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);      // 401
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"success":false,"login":true,"message":"Please sign in to continue."}""");
            log.debug("SF GATE  401 (ajax) uri='{}'", WebSecurityUtils.sanitizeForLog(uri));
            return false;
        }

        response.sendRedirect(request.getContextPath() + LOGIN_PAGE + "?redirect=" + encodedTarget(request));
        log.debug("SF GATE  302 → login  uri='{}'", WebSecurityUtils.sanitizeForLog(uri));
        return false;
    }

    /**
     * Build the ?redirect= value from the request the visitor was actually
     * trying to reach — path + query string, validated as a same-site path, then
     * URL-encoded so it survives being a query parameter.
     */
    private String encodedTarget(HttpServletRequest request) {
        String uri   = request.getRequestURI();
        String ctx   = request.getContextPath();
        String query = request.getQueryString();

        // Strip the context path so the stored value is app-relative.
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx))
                ? uri.substring(ctx.length())
                : uri;

        String target = (query != null && !query.isBlank()) ? path + "?" + query : path;

        // Fail closed: anything that does not validate as a safe same-site path
        // becomes the account dashboard rather than being reflected back.
        String safe = WebSecurityUtils.safeRedirect(target, "/account/dashboard");
        return URLEncoder.encode(safe, StandardCharsets.UTF_8);
    }
}
