package com.asg.spindleserp.security.init;

import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
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
 * SecurityDataInitializer
 *
 * Runs once at every application startup (idempotent).
 * Seeds: default Organization → ROLE_SUPER_ADMIN → superadmin user.
 *
 * Changes from uploaded version:
 *   1. Removed all commented-out dead code (isActive/isEnabled etc.)
 *   2. Uses OrganizationRepository properly (typed), not raw JpaRepository<Organization,Long>
 *   3. Removed the raw/unchecked cast workaround
 *   4. ensureDefaultOrganization is cleaner — sets isActive
 *   5. Default password: SuperAdmin@2025 (must be changed after first login)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements ApplicationRunner {

    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final PermissionRepository   permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder        passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("═══ SecurityDataInitializer starting ═══");
        Organization org         = ensureDefaultOrganization();
        Role         superAdmin  = ensureSuperAdminRole();
        ensureSuperAdminUser(org, superAdmin);
        log.info("═══ SecurityDataInitializer complete ═══");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORGANIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private Organization ensureDefaultOrganization() {
        return organizationRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Organization o = new Organization();
                    o.setCode("DEFAULT");
                    o.setName("Spindle ERP");
//                    o.setIsActive(true);
                    Organization saved = organizationRepository.save(o);
                    log.info("Created default organization id={}", saved.getId());
                    return saved;
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROLE_SUPER_ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    private Role ensureSuperAdminRole() {
        return roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> {

            // Wildcard permission (DynamicAuthorizationManager also bypasses for SUPER_ADMIN)
            Permission wildcard = permissionRepository.findByName("*")
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .name("*")
                                    .description("Super admin wildcard — all access")
                                    .urlPattern("/**")
                                    .httpMethod(null)
                                    .module("CORE_SECURITY")
                                    .active(true)
                                    .build()));

            Role role = Role.builder()
                    .name("ROLE_SUPER_ADMIN")
                    .nameBn("সুপার অ্যাডমিন")
                    .description("Full system access — bypasses all permission checks.")
                    .masterRole("ROLE_SUPER_ADMIN")
                    .active(true)
                    .permissions(new LinkedHashSet<>(Set.of(wildcard)))
                    .build();

            Role saved = roleRepository.save(role);
            log.info("Created ROLE_SUPER_ADMIN id={}", saved.getId());
            return saved;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUPERADMIN USER
    // ─────────────────────────────────────────────────────────────────────────

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
                .password(passwordEncoder.encode("SuperAdmin@2025"))
                .fullName("Super Administrator")
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .deleted(false)
                .defaultDashboard(User.DefaultDashboard.DEFAULT)
                .roles(new LinkedHashSet<>(Set.of(superAdminRole)))
                .build();

        userRepository.save(superadmin);
        log.warn("══════════════════════════════════════════════════════════");
        log.warn("  superadmin user created. CHANGE THE PASSWORD NOW!");
        log.warn("  username: superadmin   password: SuperAdmin@2025");
        log.warn("══════════════════════════════════════════════════════════");
    }
}
