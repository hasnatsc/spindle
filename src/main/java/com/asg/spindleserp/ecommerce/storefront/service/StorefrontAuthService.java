// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontAuthService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.customerSupport.repository.EcCustomerRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAuthDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * StorefrontAuthService — lightweight, session-based authentication for EcCustomer.
 *
 * Deliberately NOT wired into Spring Security's filter chain or sec_users.
 * EcCustomer is a portal-only registration (storefront self-signup); there is
 * no ERP session context at registration time (see EcCustomer entity comment).
 *
 * Session key: "SF_CUSTOMER_ID" — set on login/register, cleared on logout.
 * Cart merge on login is handled by StorefrontCartService.mergeGuestCartOnLogin().
 *
 * v2 changes:
 *  - login(): now @Transactional (writes last_login_at) + ec_customer_login_history row.
 *  - register(): also records a login-history row.
 *  - updateProfile(): the profile POST previously mutated a detached entity and
 *    never persisted — this is the proper transactional path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontAuthService {

    public static final String SESSION_CUSTOMER_ID = "SF_CUSTOMER_ID";

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?880|0)1[3-9]\\d{8}$");
    private static final Pattern EMAIL_PATTERN  = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final EcCustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // ── REGISTER ─────────────────────────────────────────────────────────────
    @Transactional
    public SfAuthDTO register(SfAuthDTO dto, HttpServletRequest request) {
        Long orgId = ContextProvider.getOrganizationId();
        String phone = normalizePhone(dto.getPhone());

        if (!PHONE_PATTERN.matcher(phone).matches())
            throw new IllegalArgumentException("Please enter a valid Bangladeshi mobile number.");
        if (customerRepository.existsByOrganizationIdAndPhone(orgId, phone))
            throw new IllegalArgumentException("An account with this phone number already exists.");
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !EMAIL_PATTERN.matcher(dto.getEmail()).matches())
            throw new IllegalArgumentException("Please enter a valid email address.");
        if (dto.getPassword() == null || dto.getPassword().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (dto.getFirstName() == null || dto.getFirstName().isBlank())
            throw new IllegalArgumentException("Please enter your name.");

        String code = "CUST-" + System.currentTimeMillis() % 1000000;

        EcCustomer customer = EcCustomer.builder()
                .organizationId(orgId)
                .customerCode(code)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName() != null ? dto.getLastName().trim() : null)
                .fullName((dto.getFirstName().trim() + " " +
                        (dto.getLastName() != null ? dto.getLastName().trim() : "")).trim())
                .email(dto.getEmail() != null && !dto.getEmail().isBlank() ? dto.getEmail().trim().toLowerCase() : null)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .emailVerified(false)
                .phoneVerified(false)
                .accountStatus(EcCustomer.AccountStatus.ACTIVE)
                .active(true)
                .deleted(false)
                .build();

        customer = customerRepository.save(customer);
        loginSession(request, customer);
        recordLogin(customer.getId(), request, "SUCCESS");

        log.info("Storefront registration: customer #{} phone={}", customer.getId(), phone);
        return toAuthDTO(customer);
    }

    // ── LOGIN ────────────────────────────────────────────────────────────────
    @Transactional
    public SfAuthDTO login(String identifier, String password, HttpServletRequest request) {
        Long orgId = ContextProvider.getOrganizationId();
        String normalized = identifier.contains("@") ? identifier.trim().toLowerCase() : normalizePhone(identifier);

        EcCustomer customer = (identifier.contains("@")
                ? customerRepository.findByOrganizationIdAndEmail(orgId, normalized)
                : customerRepository.findByOrganizationIdAndPhone(orgId, normalized))
                .orElseThrow(() -> new IllegalArgumentException("Invalid phone/email or password."));

        if (customer.isDeleted() || !customer.isActive())
            throw new IllegalArgumentException("This account is no longer active.");
        if (customer.getAccountStatus() == EcCustomer.AccountStatus.BLOCKED)
            throw new IllegalArgumentException("This account has been blocked. Please contact support.");
        if (customer.getPasswordHash() == null || !passwordEncoder.matches(password, customer.getPasswordHash())) {
            recordLogin(customer.getId(), request, "FAILED");
            throw new IllegalArgumentException("Invalid phone/email or password.");
        }

        customer.setLastLoginAt(LocalDateTime.now());
        customerRepository.save(customer);

        loginSession(request, customer);
        recordLogin(customer.getId(), request, "SUCCESS");
        log.info("Storefront login: customer #{}", customer.getId());
        return toAuthDTO(customer);
    }

    // ── LOGOUT ───────────────────────────────────────────────────────────────
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(SESSION_CUSTOMER_ID);
    }

    // ── CURRENT CUSTOMER ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public EcCustomer currentCustomerOrNull(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Long customerId = (Long) session.getAttribute(SESSION_CUSTOMER_ID);
        if (customerId == null) return null;
        return customerRepository.findById(customerId).filter(c -> !c.isDeleted() && c.isActive()).orElse(null);
    }

    public boolean isLoggedIn(HttpServletRequest request) {
        return currentCustomerOrNull(request) != null;
    }

    // ── UPDATE PROFILE ───────────────────────────────────────────────────────
    @Transactional
    public EcCustomer updateProfile(Long customerId, String firstName, String lastName, String email) {
        EcCustomer customer = customerRepository.findById(customerId)
                .filter(c -> !c.isDeleted() && c.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        if (firstName == null || firstName.isBlank())
            throw new IllegalArgumentException("Please enter your first name.");
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Please enter a valid email address.");

        customer.setFirstName(firstName.trim());
        customer.setLastName(lastName != null && !lastName.isBlank() ? lastName.trim() : null);
        customer.setFullName((customer.getFirstName() + " " +
                (customer.getLastName() != null ? customer.getLastName() : "")).trim());
        customer.setEmail(email != null && !email.isBlank() ? email.trim().toLowerCase() : null);
        return customerRepository.save(customer);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private void loginSession(HttpServletRequest request, EcCustomer customer) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CUSTOMER_ID, customer.getId());
    }

    /** Best-effort audit row in ec_customer_login_history — never fails the login. */
    private void recordLogin(Long customerId, HttpServletRequest request, String status) {
        try {
            String ua = request.getHeader("User-Agent");
            if (ua != null && ua.length() > 150) ua = ua.substring(0, 150);
            String fwd = request.getHeader("X-Forwarded-For");
            String ip = (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : request.getRemoteAddr();
            jdbcTemplate.update("""
                    INSERT INTO ec_customer_login_history
                        (customer_id, login_time, ip_address, browser, login_source, login_status)
                    VALUES (?, now(), ?, ?, 'WEB', ?)
                    """, customerId, ip, ua, status);
        } catch (Exception e) {
            log.warn("recordLogin failed: {}", e.getMessage());
        }
    }

    private String normalizePhone(String raw) {
        if (raw == null) return "";
        String p = raw.trim().replaceAll("[\\s-]", "");
        if (p.startsWith("+880")) return "0" + p.substring(4);
        if (p.startsWith("880"))  return "0" + p.substring(3);
        return p;
    }

    private SfAuthDTO toAuthDTO(EcCustomer c) {
        return SfAuthDTO.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .build();
    }
}
