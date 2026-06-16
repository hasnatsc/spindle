package com.asg.spindleserp.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig
 *
 * ══════════════════════════════════════════════════════════════════════
 * FIX 2 — Explicit static resource handler registration
 * ══════════════════════════════════════════════════════════════════════
 *
 * WHY THIS IS NEEDED:
 *   Spring Boot auto-configures a ResourceHttpRequestHandler that serves
 *   files from classpath:/static/, classpath:/public/, etc. However,
 *   when Spring Security's filter chain is active AND the path-matchers
 *   for static resources are not recognised as resource-handler paths,
 *   the request falls through to DispatcherServlet and gets routed to a
 *   @Controller — in this case UserController's catch-all, which
 *   returns "security/users-index" (HTML) instead of the JS file.
 *
 *   Explicitly registering addResourceHandlers tells DispatcherServlet:
 *   "these URL patterns are files on the classpath, not controller
 *   routes — serve them directly without consulting any controller."
 *
 * PLACEMENT:
 *   src/main/java/com/asg/spindleserp/config/WebMvcConfig.java
 *   (the config package, not the security package)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String STATIC_LOCATION = "classpath:/static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // JavaScript files
        registry.addResourceHandler("/js/**")
                .addResourceLocations(STATIC_LOCATION + "js/")
                .setCachePeriod(3600);   // 1 hour browser cache

        // CSS files
        registry.addResourceHandler("/css/**")
                .addResourceLocations(STATIC_LOCATION + "css/")
                .setCachePeriod(3600);

        // Images
        registry.addResourceHandler("/img/**")
                .addResourceLocations(STATIC_LOCATION + "img/")
                .setCachePeriod(86400);  // 24 hours

        registry.addResourceHandler("/images/**")
                .addResourceLocations(STATIC_LOCATION + "images/")
                .setCachePeriod(86400);

        // Fonts
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations(STATIC_LOCATION + "fonts/")
                .setCachePeriod(86400);

        // WebJars (Bootstrap, jQuery bundled via Maven)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // Favicons
        registry.addResourceHandler("/favicon.ico", "/favicon.svg")
                .addResourceLocations(STATIC_LOCATION);
    }
}
