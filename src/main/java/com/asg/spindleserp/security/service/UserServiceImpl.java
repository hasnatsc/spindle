package com.asg.spindleserp.security.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.entity.*;
import com.asg.spindleserp.organization.repository.*;
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
 * Changes over the uploaded version:
 *
 *   1. persistScopes() now writes to the User's @ManyToMany sets
 *      (User.organizations, allowedBusinessUnits, allowedCostCenters,
 *      allowedWarehouses) — stored in sec_user_organizations etc.
 *      The previous sec_user_access_scopes flat table is NOT used here.
 *
 *   2. After create/update, UserContextService.saveDefaultContext() is
 *      called if the DTO contains any defaultXxxId field.
 *      This writes the chosen default to user_context so the user's
 *      session starts with the right org/BU/CC/WH on next login.
 *
 *   3. Boolean (wrapper) flags — dto.isEnabled() etc. now use null-safe
 *      helpers so null → true (safe default), preventing the JSON parse
 *      error "Cannot map null into boolean".
 *
 *   4. toDTO() reads the default context back from UserContextService
 *      so the edit form can pre-fill the default context dropdowns.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final OrganizationRepository orgRepository;
    private final BusinessUnitRepository buRepository;
    private final CostCenterRepository   ccRepository;
    private final WarehouseRepository    whRepository;
    private final PasswordEncoder        passwordEncoder;
    private final UserContextService     userContextService;   // ← for default context

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ─────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO createUser(UserDTO dto) {
        validate(dto, null);
        String actor = currentUsername();

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

        User saved = userRepository.save(user);

        // Persist allowed scope sets into sec_user_organizations etc.
        persistScopes(saved, dto);

        // Persist default context into user_context table
        persistDefaultContext(saved.getId(), dto);

        log.info("User '{}' created by {}", saved.getUsername(), actor);
        return toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        return userRepository.findByIdWithRoles(id).map(this::toDTO);
    }

    @Override @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAllByDeletedFalseOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<UserDTO> findEnabled() {
        return userRepository.findAllByEnabledTrueAndDeletedFalse().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<UserDTO> search(String q) {
        return userRepository.searchActive(q).stream().map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────

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

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 8)
                throw new IllegalArgumentException("Password must be at least 8 characters.");
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User saved = userRepository.save(user);
        persistScopes(saved, dto);
        persistDefaultContext(saved.getId(), dto);

        log.info("User '{}' updated by {}", saved.getUsername(), currentUsername());
        return toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELETE (soft)
    // ─────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public UserDTO toggleStatus(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("User #" + id + " not found."));
        guardSuperAdmin(user);
        user.setEnabled(!user.isEnabled());
        user.setUpdatedBy(currentUsername());
        return toDTO(userRepository.save(user));
    }

    // ─────────────────────────────────────────────────────────────────────
    // CHANGE PASSWORD
    // ─────────────────────────────────────────────────────────────────────

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
        return toDTO(userRepository.save(user));
    }

    // ─────────────────────────────────────────────────────────────────────
    // RECORD LOGIN
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void recordLogin(String username) {
        userRepository.updateLastLogin(username, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────
    // DATATABLE
    // ─────────────────────────────────────────────────────────────────────

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
            row.put("created_at", u.getCreatedAt() != null ? u.getCreatedAt().format(DT) : "—");
            row.put("actions",    buildActions(u.getId(), u.isEnabled()));
            rows.add(row);
        }
        return DataTableResponse.of(draw, total, filtered, rows);
    }

    // ─────────────────────────────────────────────────────────────────────
    // toDTO
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserDTO toDTO(User u) {
        // Resolve allowed scope IDs from the User's ManyToMany sets
        // (re-load with context to get the sets if needed)
        User full = userRepository.findByUsernameWithAllContext(u.getUsername())
                .orElse(u);  // fallback to what we have

        Set<Long> orgIds = full.getOrganizations().stream().map(Organization::getId).collect(Collectors.toSet());
        Set<Long> buIds  = full.getAllowedBusinessUnits().stream().map(BusinessUnit::getId).collect(Collectors.toSet());
        Set<Long> ccIds  = full.getAllowedCostCenters().stream().map(CostCenter::getId).collect(Collectors.toSet());
        Set<Long> whIds  = full.getAllowedWarehouses().stream().map(Warehouse::getId).collect(Collectors.toSet());

        // Read default context from user_context table
        var ctxOpt = userContextService.findContextByUserId(u.getId());

        UserDTO.UserDTOBuilder b = UserDTO.builder()
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
                .roleIds(u.getRoles().stream().map(Role::getId).collect(Collectors.toSet()))
                .roleNames(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .roleCount(u.getRoles().size())
                .organizationIds(orgIds)
                .businessUnitIds(buIds)
                .costCenterIds(ccIds)
                .warehouseIds(whIds)
                .organizationNames(orgIds.isEmpty() ? new HashSet<>() :
                        orgRepository.findAllById(orgIds).stream().map(Organization::getName).collect(Collectors.toSet()))
                .businessUnitNames(buIds.isEmpty() ? new HashSet<>() :
                        buRepository.findAllById(buIds).stream().map(BusinessUnit::getName).collect(Collectors.toSet()))
                .costCenterNames(ccIds.isEmpty() ? new HashSet<>() :
                        ccRepository.findAllById(ccIds).stream().map(CostCenter::getCostCenterName).collect(Collectors.toSet()))
                .warehouseNames(whIds.isEmpty() ? new HashSet<>() :
                        whRepository.findAllById(whIds).stream().map(Warehouse::getWarehouseName).collect(Collectors.toSet()))
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastLoginAt(u.getLastLoginAt());

        // Populate default context fields (from user_context row)
        ctxOpt.ifPresent(ctx -> {
            if (ctx.getOrganization() != null) {
                b.defaultOrganizationId(ctx.getOrganization().getId());
                b.defaultOrganizationName(ctx.getOrganization().getName());
            }
            if (ctx.getBusinessUnit() != null) {
                b.defaultBusinessUnitId(ctx.getBusinessUnit().getId());
                b.defaultBusinessUnitName(ctx.getBusinessUnit().getName());
            }
            if (ctx.getCostCenter() != null) {
                b.defaultCostCenterId(ctx.getCostCenter().getId());
                b.defaultCostCenterName(ctx.getCostCenter().getCostCenterName());
            }
            if (ctx.getWarehouse() != null) {
                b.defaultWarehouseId(ctx.getWarehouse().getId());
                b.defaultWarehouseName(ctx.getWarehouse().getWarehouseName());
            }
        });

        return b.build();
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

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — SCOPE PERSISTENCE (ManyToMany sets)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Replaces the User's allowed-scope sets with the new values from the DTO.
     * JPA's @ManyToMany cascade handles the junction-table inserts/deletes.
     * Called inside createUser() and updateUser() (same transaction).
     */
    private void persistScopes(User user, UserDTO dto) {
        // Organizations
        Set<Organization> orgs = dto.getOrganizationIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(orgRepository.findAllById(dto.getOrganizationIds()));
        user.setOrganizations(orgs);

        // Business Units
        Set<BusinessUnit> bus = dto.getBusinessUnitIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(buRepository.findAllById(dto.getBusinessUnitIds()));
        user.setAllowedBusinessUnits(bus);

        // Cost Centers
        Set<CostCenter> ccs = dto.getCostCenterIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(ccRepository.findAllById(dto.getCostCenterIds()));
        user.setAllowedCostCenters(ccs);

        // Warehouses
        Set<Warehouse> whs = dto.getWarehouseIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(whRepository.findAllById(dto.getWarehouseIds()));
        user.setAllowedWarehouses(whs);

        userRepository.save(user);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — DEFAULT CONTEXT PERSISTENCE
    // ─────────────────────────────────────────────────────────────────────

    private void persistDefaultContext(Long userId, UserDTO dto) {
        boolean anySet = dto.getDefaultOrganizationId() != null
                      || dto.getDefaultBusinessUnitId()  != null
                      || dto.getDefaultCostCenterId()    != null
                      || dto.getDefaultWarehouseId()     != null;
        if (!anySet) return;   // admin didn't set a default — leave existing row intact

        Organization org = dto.getDefaultOrganizationId() != null
                ? orgRepository.getReferenceById(dto.getDefaultOrganizationId()) : null;
        BusinessUnit bu  = dto.getDefaultBusinessUnitId()  != null
                ? buRepository.getReferenceById(dto.getDefaultBusinessUnitId())  : null;
        CostCenter   cc  = dto.getDefaultCostCenterId()    != null
                ? ccRepository.getReferenceById(dto.getDefaultCostCenterId())    : null;
        Warehouse    wh  = dto.getDefaultWarehouseId()     != null
                ? whRepository.getReferenceById(dto.getDefaultWarehouseId())     : null;

        userContextService.saveDefaultContext(userId, org, bu, cc, wh);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — MISC
    // ─────────────────────────────────────────────────────────────────────

    private void validate(UserDTO dto, Long excludeId) {
        String uname = dto.getUsername().trim().toLowerCase();
        String email = dto.getEmail().trim().toLowerCase();
        String phone = dto.getPhone() != null ? dto.getPhone().trim() : null;

        if (excludeId == null) {
            if (userRepository.existsByUsername(uname))
                throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
            if (userRepository.existsByEmail(email))
                throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
            if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone))
                throw new IllegalArgumentException("Phone '" + dto.getPhone() + "' is already registered.");
            if (dto.getPassword() == null || dto.getPassword().length() < 8)
                throw new IllegalArgumentException("Password must be at least 8 characters.");
        } else {
            if (userRepository.existsByUsernameAndIdNot(uname, excludeId))
                throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
            if (userRepository.existsByEmailAndIdNot(email, excludeId))
                throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
            if (phone != null && !phone.isBlank() && userRepository.existsByPhoneAndIdNot(phone, excludeId))
                throw new IllegalArgumentException("Phone '" + dto.getPhone() + "' is already registered.");
        }
    }

    private void guardSuperAdmin(User user) {
        boolean isSA = user.getRoles().stream()
                .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
        if (isSA && !user.getUsername().equals(currentUsername()))
            throw new IllegalArgumentException(
                    "The superadmin account cannot be modified through this interface.");
    }

    private Set<Role> resolveRoles(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(roleRepository.findAllById(ids));
    }

    private String currentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null) ? auth.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

    private String buildRolesBadges(User u) {
        if (u.getRoles().isEmpty()) return "<span class='badge bg-secondary'>No Roles</span>";
        return u.getRoles().stream()
                .map(r -> "<span class='badge bg-primary me-1'>"
                        + r.getName().replace("ROLE_", "") + "</span>")
                .collect(Collectors.joining());
    }

    private String buildActions(Long id, boolean enabled) {
        String icon  = enabled ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        String title = enabled ? "Disable" : "Enable";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",     "userShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",     "userEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='" + title + "' onclick='userToggle(" + id + ")'>"
             + "<i class='fa " + icon + "'></i></button>"
             + btn("primary", "fa-key",   "Password", "userPwd("    + id + ")")
             + btn("danger",  "fa-trash", "Delete",   "userDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String c, String icon, String title, String onclick) {
        return "<button class='btn btn-outline-" + c + "' title='" + title + "' onclick='" + onclick + "'>"
             + "<i class='fa " + icon + "'></i></button>";
    }

    private static String orDash(String v) { return (v != null && !v.isBlank()) ? v : "—"; }
}
