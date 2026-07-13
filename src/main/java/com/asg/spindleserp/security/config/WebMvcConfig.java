package com.asg.spindleserp.security.config;

import com.asg.spindleserp.ecommerce.storefront.StorefrontPaths;
import com.asg.spindleserp.ecommerce.storefront.security.StorefrontAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig
 *
 * ══════════════════════════════════════════════════════════════════════════
 * RETAINED — explicit static resource handler registration
 * ══════════════════════════════════════════════════════════════════════════
 * Spring Boot auto-configures a ResourceHttpRequestHandler for classpath:/static
 * etc., but when Spring Security's filter chain is active and the static paths
 * are not recognised as resource-handler paths, the request falls through to
 * DispatcherServlet and gets routed to a @Controller — in this app, previously
 * to UserController's catch-all, which returned "security/users-index" (HTML)
 * where a .js file was expected. Registering these explicitly tells
 * DispatcherServlet "these are files, not controller routes".
 *
 * ══════════════════════════════════════════════════════════════════════════
 * NEW — StorefrontAuthInterceptor registration
 * ══════════════════════════════════════════════════════════════════════════
 * This is what turns the storefront login check from a convention (a hand-copied
 * `if (currentCustomerOrNull(request) == null) return "redirect:…"` at the top of
 * twelve handler methods across four controllers) into an enforced control.
 *
 * Spring Security cannot do this job: storefront customers are EcCustomer rows
 * identified by a session attribute, with no sec_users row and no Authentication,
 * so .requestMatchers("/account/**").authenticated() would lock out every real
 * customer. A handler interceptor is the correct layer — it runs after Spring
 * Security has (correctly) waved the request through as permitAll, and before any
 * controller method executes.
 *
 * Path patterns come from StorefrontPaths, the same single source of truth that
 * SecurityConfig and StorefrontOrgContextFilter use.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String STATIC_LOCATION = "classpath:/static/";

    private final StorefrontAuthInterceptor storefrontAuthInterceptor;

    @Value("${app.upload-dir}")
    private String uploadDir;

    // ── Interceptors ─────────────────────────────────────────────────────────

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storefrontAuthInterceptor)
                .addPathPatterns(StorefrontPaths.CUSTOMER_ONLY)      // /account/**, /checkout/**, /wishlist/**
                .excludePathPatterns(StorefrontPaths.CUSTOMER_OPEN); // login, register, logout, password reset
    }

    // ── Static resources ─────────────────────────────────────────────────────

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // User-uploaded files.
        //
        // ★ Note on path traversal: Spring's ResourceHttpRequestHandler already
        //   refuses to resolve a path that escapes its configured location
        //   (PathResourceResolver.checkResource → isResourceUnderLocation), so
        //   /uploads/../../../../etc/passwd is rejected here. That protection is
        //   NOT automatic for the app's own download endpoints, which read a
        //   filename out of a DB row and open it directly — those need their own
        //   canonical-path check, which the project's existing convention already
        //   applies. Worth stating so the difference is not assumed away.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizeDir(uploadDir));

        registry.addResourceHandler("/js/**")
                .addResourceLocations(STATIC_LOCATION + "js/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/css/**")
                .addResourceLocations(STATIC_LOCATION + "css/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/img/**")
                .addResourceLocations(STATIC_LOCATION + "img/")
                .setCachePeriod(86400);

        registry.addResourceHandler("/images/**")
                .addResourceLocations(STATIC_LOCATION + "images/")
                .setCachePeriod(86400);

        registry.addResourceHandler("/fonts/**")
                .addResourceLocations(STATIC_LOCATION + "fonts/")
                .setCachePeriod(86400);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        registry.addResourceHandler("/favicon.ico", "/favicon.svg")
                .addResourceLocations(STATIC_LOCATION);
    }

    /**
     * A "file:" resource location MUST end with a separator, or Spring treats the
     * last path segment as a filename prefix and silently serves the wrong thing
     * (e.g. app.upload-dir=/var/www/uploads makes /uploads/x.png resolve against
     * /var/www/uploadsx.png). The original hard-coded the trailing slash, which
     * meant a configured value that already ended in "/" produced a double slash.
     * Harmless on Linux, not guaranteed elsewhere. Normalise it once, here.
     */
    private static String normalizeDir(String dir) {
        if (dir == null || dir.isBlank()) return "./";
        String d = dir.replace('\\', '/').trim();
        return d.endsWith("/") ? d : d + "/";
    }
}
