// Path: com/asg/spindleserp/ecommerce/storefront/controller/StorefrontSiteController.java
package com.asg.spindleserp.ecommerce.storefront.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * StorefrontSiteController — the theme's static site pages.
 * Pages: /about  /contact  /faq
 * REST:  POST /contact/send
 *
 * ★ /contact/send currently logs the message and returns success — there is
 *   no ec_contact_messages table in the schema and inventing one wasn't in
 *   scope. Wire this to email (JavaMailSender) or a CRM lead
 *   (crm_leads insert) when ready; the payload shape is already final:
 *   {name, contact, subject, message}.
 */
@Slf4j
@Controller
public class StorefrontSiteController {

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("activeNav", "about");
        return "ecommerce/storefront/sf-about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("activeNav", "contact");
        return "ecommerce/storefront/sf-contact";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("activeNav", "faq");
        return "ecommerce/storefront/sf-faq";
    }

    @PostMapping("/contact/send")
    @ResponseBody
    public Map<String, Object> contactSend(@RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        String name = trimOrNull(body.get("name"));
        String contact = trimOrNull(body.get("contact"));
        String message = trimOrNull(body.get("message"));
        if (name == null || contact == null || message == null) {
            res.put("success", false);
            res.put("message", "Please fill in your name, contact and message.");
            return res;
        }
        log.info("Storefront contact message — name: {}, contact: {}, subject: {}, message: {}",
                name, contact, body.get("subject"), message);
        res.put("success", true);
        res.put("message", "Thanks — we'll get back to you shortly.");
        return res;
    }

    private static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
