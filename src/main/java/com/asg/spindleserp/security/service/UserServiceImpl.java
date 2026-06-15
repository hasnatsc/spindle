package com.asg.spindleserp.security.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.dto.UserDTO;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.RoleRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserServiceImpl
 *
 * Changes from the uploaded version:
 *
 * 1. existsByUsername/existsByEmail changed to:
 *      existsByUsernameAndIdNot / existsByEmailAndIdNot  (update)
 *      existsByUsername / existsByEmail                  (create)
 *    The custom JPQL overloads in the uploaded UserRepository have been
 *    replaced with the standard Spring Data method-name conventions that
 *    already exist in the project repository.
 *
 * 2. datatableList uses DataTableResponse.of() (project's factory method).
 *    Pagination is in-memory (full table to page). Replace with
 *    Pageable/Specification when user count exceeds ~5 000.
 *
 * 3. buildActions() uses the JS function names that match users-index.html:
 *    userShow / userEdit / userToggle / userPwd / userDelete
 *
 * 4. phone uniqueness check added to createUser / updateUser.
 *
 * 5. createUser sets organization via the auth context (superadmin shares org).
 *    For a multi-org system, pass orgId in the DTO.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO createUser(UserDTO dto) {
        validate(dto, null);

        String actor = currentUsername();

        // Resolve organization from the logged-in user's context
        User currentUser = userRepository
                .findByUsernameWithRolesAndPermissions(actor)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + actor));

        User user = User.builder()
                .organization(currentUser.getOrganization())
                .username(dto.getUsername().trim().toLowerCase())
                .email(dto.getEmail().trim().toLowerCase())
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : null)
                .fullName(dto.getFullName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(dto.isEnabled())
                .accountNonExpired(dto.isAccountNonExpired())
                .accountNonLocked(dto.isAccountNonLocked())
                .credentialsNonExpired(dto.isCredentialsNonExpired())
                .defaultDashboard(dto.getDefaultDashboard())
                .roles(resolveRoles(dto.getRoleIds()))
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        UserDTO saved = toDTO(userRepository.save(user));
        log.info("User '{}' created by {}", saved.getUsername(), actor);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        return userRepository.findByIdWithRoles(id).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findEnabled() {
        return userRepository.findAllByEnabledTrueAndDeletedFalse()
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> search(String query) {
        return userRepository.searchActive(query)
                .stream().map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new IllegalArgumentException("User #" + id + " not found."));

        validate(dto, id);

        user.setUsername(dto.getUsername().trim().toLowerCase());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setFullName(dto.getFullName());
        user.setEnabled(dto.isEnabled());
        user.setAccountNonExpired(dto.isAccountNonExpired());
        user.setAccountNonLocked(dto.isAccountNonLocked());
        user.setCredentialsNonExpired(dto.isCredentialsNonExpired());
        user.setDefaultDashboard(dto.getDefaultDashboard());
        user.setRoles(resolveRoles(dto.getRoleIds()));
        user.setUpdatedBy(currentUsername());

        // Password update — only when provided
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 8)
                throw new IllegalArgumentException("Password must be at least 8 characters.");
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        UserDTO saved = toDTO(userRepository.save(user));
        log.info("User '{}' updated by {}", saved.getUsername(), currentUsername());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE (soft)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("User #" + id + " not found."));

        guardSuperAdmin(user);

        user.setDeleted(true);
        user.setEnabled(false);
        user.setUpdatedBy(currentUsername());
        userRepository.save(user);
        log.info("User #{} '{}' soft-deleted by {}", id, user.getUsername(), currentUsername());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO toggleStatus(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("User #" + id + " not found."));

        guardSuperAdmin(user);

        user.setEnabled(!user.isEnabled());
        user.setUpdatedBy(currentUsername());
        UserDTO result = toDTO(userRepository.save(user));
        log.info("User #{} '{}' {} by {}", id, user.getUsername(),
                result.isEnabled() ? "enabled" : "disabled", currentUsername());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE PASSWORD
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO changePassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.isBlank())
            throw new IllegalArgumentException("New password is required.");
        if (newPassword.length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");

        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("User #" + id + " not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedBy(currentUsername());
        log.info("Password changed for user #{} '{}' by {}", id, user.getUsername(), currentUsername());
        return toDTO(userRepository.save(user));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECORD LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void recordLogin(String username) {
        userRepository.updateLastLogin(username, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE (server-side, in-memory pagination)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {

        String q = (search == null) ? "" : search.trim();

        List<User> all = q.isBlank()
                ? userRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
                : userRepository.searchActive(q);

        long total    = userRepository.countByDeletedFalse();
        long filtered = q.isBlank() ? total : all.size();

        List<User> page = all.stream().skip(start).limit(length).toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;
        for (User u : page) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sl",         sl++);
            row.put("username",   u.getUsername());
            row.put("full_name",  orDash(u.getFullName()));
            row.put("email",      u.getEmail());
            row.put("phone",      orDash(u.getPhone()));
            row.put("roles",      buildRolesBadges(u));
            row.put("status",     u.isEnabled()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Disabled</span>");
            row.put("created_at", u.getCreatedAt() != null
                    ? u.getCreatedAt().format(DT) : "—");
            row.put("actions",    buildActions(u.getId(), u.isEnabled()));
            rows.add(row);
        }

        return DataTableResponse.of(draw, total, filtered, rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONVERSION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO toDTO(User u) {
        Set<Long>   roleIds   = u.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        Set<String> roleNames = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return UserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phone(u.getPhone())
                .fullName(u.getFullName())
                .defaultDashboard(u.getDefaultDashboard())
                .enabled(u.isEnabled())
                .accountNonExpired(u.isAccountNonExpired())
                .accountNonLocked(u.isAccountNonLocked())
                .credentialsNonExpired(u.isCredentialsNonExpired())
                .roleIds(roleIds)
                .roleNames(roleNames)
                .roleCount(roleIds.size())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }

    @Override
    public User toEntity(UserDTO dto) {
        return User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .fullName(dto.getFullName())
                .defaultDashboard(dto.getDefaultDashboard())
                .enabled(dto.isEnabled())
                .roles(resolveRoles(dto.getRoleIds()))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Centralized validation for create (excludeId = null) and update (excludeId = id).
     */
    private void validate(UserDTO dto, Long excludeId) {
        if (excludeId == null) {
            // CREATE
            if (userRepository.existsByUsername(dto.getUsername().trim().toLowerCase()))
                throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
            if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase()))
                throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
            if (dto.getPhone() != null && !dto.getPhone().isBlank()
                    && userRepository.existsByPhone(dto.getPhone().trim()))
                throw new IllegalArgumentException("Phone '" + dto.getPhone() + "' is already registered.");
            if (dto.getPassword() == null || dto.getPassword().length() < 8)
                throw new IllegalArgumentException("Password must be at least 8 characters.");
        } else {
            // UPDATE — exclude self
            if (userRepository.existsByUsernameAndIdNot(dto.getUsername().trim().toLowerCase(), excludeId))
                throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
            if (userRepository.existsByEmailAndIdNot(dto.getEmail().trim().toLowerCase(), excludeId))
                throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
            if (dto.getPhone() != null && !dto.getPhone().isBlank()
                    && userRepository.existsByPhoneAndIdNot(dto.getPhone().trim(), excludeId))
                throw new IllegalArgumentException("Phone '" + dto.getPhone() + "' is already registered.");
        }
    }

    /** Prevent mutating the superadmin account via the UI. */
    private void guardSuperAdmin(User user) {
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
        if (isSuperAdmin) {
            String current = currentUsername();
            if (!user.getUsername().equals(current))
                throw new IllegalArgumentException(
                        "The superadmin account cannot be modified through this interface.");
        }
    }

    private Set<Role> resolveRoles(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(roleRepository.findAllById(ids));
    }

    private String currentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null) ? auth.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }

    private String buildRolesBadges(User u) {
        if (u.getRoles().isEmpty())
            return "<span class='badge bg-secondary'>No Roles</span>";
        return u.getRoles().stream()
                .map(r -> "<span class='badge bg-primary me-1'>"
                        + r.getName().replace("ROLE_", "") + "</span>")
                .collect(Collectors.joining());
    }

    /**
     * Action buttons — names match the JS functions in users-index.html:
     * userShow / userEdit / userToggle / userPwd / userDelete
     */
    private String buildActions(Long id, boolean enabled) {
        String toggleIcon  = enabled ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        String toggleTitle = enabled ? "Disable" : "Enable";
        return "<div class='btn-group btn-group-sm' role='group'>"
             + btn("info",      "fa-eye",    "View",           "userShow("   + id + ")")
             + btn("warning",   "fa-pencil", "Edit",           "userEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='" + toggleTitle + "' "
             +   "onclick='userToggle(" + id + ")'>"
             +   "<i class='fa " + toggleIcon + "'></i></button>"
             + btn("primary",   "fa-key",    "Change Password", "userPwd("   + id + ")")
             + btn("danger",    "fa-trash",  "Delete",          "userDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String color, String icon, String title, String onclick) {
        return "<button class='btn btn-outline-" + color + "' title='" + title + "' "
             + "onclick='" + onclick + "'>"
             + "<i class='fa " + icon + "'></i></button>";
    }

    private static String orDash(String v) {
        return (v != null && !v.isBlank()) ? v : "—";
    }
}
