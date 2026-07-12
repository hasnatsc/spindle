package com.asg.spindleserp.security.init;

import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.entity.Permission;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.PermissionRepository;
import com.asg.spindleserp.security.repository.RoleRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization organization = ensureDefaultOrganization();
        Organization sbmGreenOrganization = ensureSBMGreenOrganization();
        Role superAdminRole = ensureSuperAdminRole();
        ensureRole("ROLE_ADMIN", "System Administrator");
        ensureRole("ROLE_CORE_SECURITY_MANAGER", "Core Security Manager");
        ensureRole("ROLE_HRM_MANAGER", "HR Manager");
        ensureRole("ROLE_SALES_MANAGER", "Sales Manager");
        ensureRole("ROLE_PURCHASE_MANAGER", "Purchase Manager");
        ensureRole("ROLE_INVENTORY_MANAGER", "Inventory Manager");
        ensureRole("ROLE_FINANCE_MANAGER", "Finance Manager");
        ensureRole("ROLE_PRODUCTION_MANAGER", "Production Manager");
        ensureRole("ROLE_PRODUCT_MANAGER", "Product Catalog Manager");
        ensureRole("ROLE_POS_MANAGER", "POS Manager");
        ensureRole("ROLE_CRM_MANAGER", "CRM Manager");
        ensureRole("ROLE_COMMUNICATION_MANAGER", "Communication Manager");
        ensureRole("ROLE_COMMERCIAL_MANAGER", "Commercial Manager");
        ensureRole("ROLE_REPORT_MANAGER", "Reports & Analytics Manager");
        ensureRole("ROLE_BUDGET_MANAGER", "Budget Manager");
        ensureRole("ROLE_FIXED_ASSET_MANAGER", "Fixed Asset Manager");
        ensureSuperAdminUser(organization, superAdminRole);
//        ensureDefaultUsers(organization);
    }

// ============================================================
// ORGANIZATION
// ============================================================

    private Organization ensureDefaultOrganization() {
        Optional<Organization> existing = organizationRepository.findFirstByOrderByIdAsc();
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDateTime now = LocalDateTime.now();
        Organization org = Organization.builder().code("SE").name("Spindles ERP").nameBn("স্পিন্ডলস ইআরপি").about("Default organization created automatically during system initialization.").address("Head Office").city("Dhaka").state("Dhaka").country("Bangladesh").postalCode("1000").phone("+8801000000000").email("admin@spindleserp.com").website("https://spindleserp.com").establishedDate(LocalDate.now()).isActive(true).createdBy("system").updatedBy("system").createdAt(now).updatedAt(now).build();
        return organizationRepository.save(org);
    }

    private Organization ensureSBMGreenOrganization() {
        return organizationRepository.findByCode("SBM").orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Organization org = Organization.builder().code("SBM").name("SBM Green").nameBn("এসবিএম গ্রীন").about("SBM Green Organization").address("Dhaka").city("Dhaka").state("Dhaka").country("Bangladesh").postalCode("1000").phone("+8801700000000").email("admin@sbmgreen.com").website("https://sbmgreen.com").establishedDate(LocalDate.now()).isActive(true).createdBy("system").updatedBy("system").createdAt(now).updatedAt(now).build();
                    return organizationRepository.save(org);
                });
    }

// ============================================================
// SUPER ADMIN ROLE
// ============================================================

    private Role ensureSuperAdminRole() {
        return roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> {
                    Permission wildcard = permissionRepository.findByName("*").orElseGet(() -> permissionRepository.save(Permission.builder().name("*").description("Super admin wildcard permission").urlPattern("/**").httpMethod(null).module("CORE_SECURITY").active(true).build()));
                    Role role = Role.builder().name("ROLE_SUPER_ADMIN").nameBn("সুপার অ্যাডমিন").description("Full system access").masterRole("ROLE_SUPER_ADMIN").active(true).permissions(new LinkedHashSet<>(Set.of(wildcard))).build();
                    return roleRepository.save(role);
                });
    }

// ============================================================
// GENERIC ROLE
// ============================================================

    private Role ensureRole(String roleName, String description) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
                    Role role = Role.builder().name(roleName).masterRole(roleName).description(description).active(true).build();
                    return roleRepository.save(role);
                });
    }

// ============================================================
// SUPER ADMIN USER
// ============================================================

    private void ensureSuperAdminUser(Organization org, Role superAdminRole) {
        if (userRepository.existsByUsername("superadmin")) {
            return;
        }
        User user = User.builder().organization(org).username("superadmin").email("superadmin@spindle.local").phone("01000000001").password(passwordEncoder.encode("SuperAdmin@2025")).fullName("Super Administrator").enabled(true).accountNonLocked(true).accountNonExpired(true).credentialsNonExpired(true).deleted(false).defaultDashboard(User.DefaultDashboard.DEFAULT).roles(new LinkedHashSet<>(Set.of(superAdminRole))).build();
        userRepository.save(user);
    }

// ============================================================
// DEFAULT USERS
// ============================================================

    private void ensureDefaultUsers(Organization org) {
        createUserIfNotExists(org, "admin", "Admin@2025", "System Administrator", "admin@spindle.local", roleRepository.findByName("ROLE_ADMIN").orElseThrow());
        createUserIfNotExists(org, "accounts", "Accounts@2025", "Accounts Manager", "accounts@spindle.local", roleRepository.findByName("ROLE_ACCOUNTS_MANAGER").orElseThrow());
        createUserIfNotExists(org, "hr", "Hr@2025", "HR Manager", "hr@spindle.local", roleRepository.findByName("ROLE_HR_MANAGER").orElseThrow());
        createUserIfNotExists(org, "purchase", "Purchase@2025", "Purchase Manager", "purchase@spindle.local", roleRepository.findByName("ROLE_PURCHASE_MANAGER").orElseThrow());
        createUserIfNotExists(org, "inventory", "Inventory@2025", "Inventory Manager", "inventory@spindle.local", roleRepository.findByName("ROLE_INVENTORY_MANAGER").orElseThrow());
        createUserIfNotExists(org, "production", "Production@2025", "Production Manager", "production@spindle.local", roleRepository.findByName("ROLE_PRODUCTION_MANAGER").orElseThrow());
        createUserIfNotExists(org, "sales", "Sales@2025", "Sales Manager", "sales@spindle.local", roleRepository.findByName("ROLE_SALES_MANAGER").orElseThrow());
        createUserIfNotExists(org, "quality", "Quality@2025", "Quality Manager", "quality@spindle.local", roleRepository.findByName("ROLE_QUALITY_MANAGER").orElseThrow());
    }

// ============================================================
// GENERIC USER CREATOR
// ============================================================

    private void createUserIfNotExists(Organization org, String username, String password, String fullName, String email, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = User.builder().organization(org).username(username).email(email).phone(generateRandomPhone()).password(passwordEncoder.encode(password)).fullName(fullName).enabled(true).accountNonLocked(true).accountNonExpired(true).credentialsNonExpired(true).deleted(false).defaultDashboard(User.DefaultDashboard.DEFAULT).roles(new LinkedHashSet<>(Set.of(role))).build();
        userRepository.save(user);
    }

    private String generateRandomPhone() {
        return "01" + (5 + new Random().nextInt(5)) + String.format("%08d", new Random().nextInt(100_000_000));
    }

}
