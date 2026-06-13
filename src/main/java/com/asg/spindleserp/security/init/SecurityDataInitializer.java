package com.asg.spindleserp.security.init;

import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.security.entity.*;
import com.asg.spindleserp.security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Runs once at startup (idempotent — safe to run on every restart).
 * Seeds:
 *   • ROLE_SUPER_ADMIN role
 *   • A wildcard permission that grants SUPER_ADMIN access to everything
 *   • superadmin user (username: superadmin  /  password: Admin@1234)
 *
 * The DynamicAuthorizationManager bypasses all URL checks for ROLE_SUPER_ADMIN
 * users, so the wildcard permission is actually redundant but good for completeness.
 *
 * Change the default password IMMEDIATELY after first login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityDataInitializer implements ApplicationRunner {

    private final UserRepository       userRepository;
    private final RoleRepository       roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder      passwordEncoder;

    // Inject via a simple JPA query — Organization must exist first
    private final org.springframework.data.jpa.repository.JpaRepository
            <Organization, Long> organizationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Running SecurityDataInitializer...");

        Organization org = ensureDefaultOrganization();
        Role superAdminRole = ensureSuperAdminRole();
        ensureSuperAdminUser(org, superAdminRole);

        log.info("SecurityDataInitializer complete.");
    }

    // ── Organization ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Organization ensureDefaultOrganization() {
        return (Organization) organizationRepository.findAll()
                .stream().findFirst().orElseGet(() -> {
                    Organization o = new Organization();
                    o.setCode("DEFAULT");
                    o.setName("Spindle ERP");
//                    o.setIsActive(true);
                    Organization saved = organizationRepository.save(o);
                    log.info("Created default organization id={}", saved.getId());
                    return saved;
                });
    }

    // ── ROLE_SUPER_ADMIN ──────────────────────────────────────────────────

    private Role ensureSuperAdminRole() {
        return roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> {
            // Wildcard permission (DynamicAuthorizationManager bypasses anyway)
            Permission all = permissionRepository.findByName("*")
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .name("*")
                                    .description("Super admin wildcard — all access")
                                    .urlPattern("/**")
                                    .httpMethod(null)   // any method
                                    .module("CORE_SECURITY")
                                    .active(true)
                                    .build()));

            Role role = Role.builder()
                    .name("ROLE_SUPER_ADMIN")
                    .nameBn("সুপার অ্যাডমিন")
                    .description("Full system access — bypasses all permission checks")
                    .masterRole("ROLE_SUPER_ADMIN")
                    .active(true)
//                    .isActive(true)
                    .permissions(new LinkedHashSet<>(Set.of(all)))
                    .build();

            Role saved = roleRepository.save(role);
            log.info("Created ROLE_SUPER_ADMIN id={}", saved.getId());
            return saved;
        });
    }

    // ── superadmin user ───────────────────────────────────────────────────

    private void ensureSuperAdminUser(Organization org, Role superAdminRole) {
        if (userRepository.existsByUsername("superadmin")) {
            log.debug("superadmin user already exists — skipping.");
            return;
        }

        User superadmin = User.builder()
                .organization(org)
                .username("superadmin")
                .email("superadmin@spindle.local")
                .phone("01000000000")
                .password(passwordEncoder.encode("Admin@1234"))
                .fullName("Super Administrator")
                .enabled(true)
//                .isEnabled(true)
                .accountNonLocked(true)
//                .isAccountNonLocked(true)
                .accountNonExpired(true)
//                .isAccountNonExpired(true)
                .credentialsNonExpired(true)
//                .isCredentialsNonExpired(true)
                .deleted(false)
                .defaultDashboard(User.DefaultDashboard.DEFAULT)
                .roles(new LinkedHashSet<>(Set.of(superAdminRole)))
                .build();

        userRepository.save(superadmin);
        log.warn("Created superadmin user — CHANGE THE DEFAULT PASSWORD IMMEDIATELY!");
    }
}
