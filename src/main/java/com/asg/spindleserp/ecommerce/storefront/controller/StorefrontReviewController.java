// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontReviewController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontReviewController.
 * REST: POST /product/{productId}/reviews {rating, title, text}
 * Reviews land as PENDING; the admin review queue approves them.
 */
@Controller
@RequiredArgsConstructor
public class StorefrontReviewController {

    private final StorefrontReviewService reviewService;
    private final StorefrontAuthService authService;

    @PostMapping("/product/{productId}/reviews")
    @ResponseBody
    public Map<String, Object> submit(@PathVariable Long productId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) {
            res.put("success", false);
            res.put("login", true);
            res.put("message", "Please sign in to write a review.");
            return res;
        }
        try {
            Integer rating = body.get("rating") != null ? Integer.valueOf(body.get("rating").toString()) : null;
            String title = body.get("title") != null ? body.get("title").toString() : null;
            String text  = body.get("text")  != null ? body.get("text").toString()  : null;
            res.put("success", true);
            res.put("message", reviewService.submit(customer, productId, rating, title, text));
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }
}
