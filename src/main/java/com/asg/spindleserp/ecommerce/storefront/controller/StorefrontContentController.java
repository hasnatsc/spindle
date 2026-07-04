// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontContentController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import com.asg.spindleserp.ecommerce.storefront.service.StorefrontContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontContentController — CMS pages + newsletter.
 * Page: /page/{slug}     (ec_pages, published only)
 * REST: POST /newsletter/subscribe {email}
 */
@Controller
@RequiredArgsConstructor
public class StorefrontContentController {

    private final StorefrontContentService contentService;

    @GetMapping("/page/{slug}")
    public String page(@PathVariable String slug, Model model) {
        Map<String, Object> page = contentService.pageBySlug(slug);
        if (page == null) return "redirect:/";
        model.addAttribute("pageTitle", page.get("page_title"));
        model.addAttribute("pageContent", page.get("page_content"));
        model.addAttribute("seoTitle", page.get("seo_title"));
        model.addAttribute("seoDescription", page.get("seo_description"));
        return "ecommerce/storefront/sf-page";
    }

    @PostMapping("/newsletter/subscribe")
    @ResponseBody
    public Map<String, Object> subscribe(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("message", contentService.subscribeNewsletter(body.get("email")));
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }
}
