package com.asg.spindleserp.config;

import com.asg.spindleserp.security.ContextProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        String username = ContextProvider.getCurrentUsername();
        return Optional.ofNullable(username != null ? username : "system");
    }
}