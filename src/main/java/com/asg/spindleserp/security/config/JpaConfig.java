package com.asg.spindleserp.security.config;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables @CreatedBy / @LastModifiedBy population on BaseEntity.
 * The current logged-in username is automatically injected into
 * created_by / updated_by columns on every INSERT / UPDATE.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("system");
            }
            if (auth.getPrincipal() instanceof CustomUserDetails ud) {
                return Optional.of(ud.getUsername());
            }
            return Optional.of(auth.getName());
        };
    }
}
