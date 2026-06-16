package com.asg.spindleserp.security.menu;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.entity.AppMenu;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.entity.RoleMenuAccess;
import com.asg.spindleserp.security.repository.AppMenuRepository;
import com.asg.spindleserp.security.repository.RoleMenuAccessRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MenuService
 *
 * Builds the navigation menu tree for the currently logged-in user.
 *
 * Two access paths for determining menu visibility:
 *   Path 1 — RoleMenuAccess:
 *     The user's role has an explicit canView=true entry for this menu.
 *   Path 2 — requiredPermission fallback:
 *     The menu's requiredPermission string is in the user's granted authorities.
 *
 * SUPER_ADMIN sees all menus.
 *
 * MODULE and GROUP items with no requiredPermission are always included
 * as structural containers (the JS hides them if they have no visible children).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final AppMenuRepository        menuRepository;
    private final RoleMenuAccessRepository roleMenuAccessRepository;
    private final UserRepository           userRepository;
    private final PasswordEncoder          passwordEncoder;

    // ── Get visible menus ─────────────────────────────────────────────────────

    /**
     * Returns the FLAT list of AppMenu items visible to this user,
     * ordered by displayOrder. The JS in topMenu.html builds the tree
     * using parentId / parentMenuId.
     *
     * @param userDetails — the currently authenticated user
     * @return ordered flat list of visible menus
     */
    public List<AppMenu> getVisibleMenus(CustomUserDetails userDetails) {

        // SUPER_ADMIN sees everything — no filtering
        if (userDetails.isSuperAdmin()) {
            return menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
        }

        // Collect role IDs for the user
        Set<Long> roleIds = userDetails.getUser().getRoles()
                .stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        // Path 1: collect menu IDs explicitly visible via RoleMenuAccess
        Set<Long> explicitMenuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            roleMenuAccessRepository
                    .findViewableByRoleId(roleId)
                    .stream()
                    .map(rma -> rma.getMenu().getId())
                    .forEach(explicitMenuIds::add);
        }

        // Path 2: collect authority names for permission-fallback check
        Set<String> userAuthorities = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        // Get all active menus and apply visibility filter
        List<AppMenu> all = menuRepository
                .findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

        return all.stream()
                .filter(menu -> isVisible(menu, explicitMenuIds, userAuthorities))
                .collect(Collectors.toList());
    }

    /**
     * Returns a map of menuId → RoleMenuAccess for the user's roles.
     * Used by pages that need to check canCreate / canEdit / canDelete.
     *
     * @param userDetails — the currently authenticated user
     */
    public Map<Long, RoleMenuAccess> getMenuAccessMap(CustomUserDetails userDetails) {
        if (userDetails.isSuperAdmin()) {
            // Super admin: synthesize full-access entries for every menu
            Map<Long, RoleMenuAccess> map = new HashMap<>();
            menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc()
                    .forEach(menu -> {
                        RoleMenuAccess rma = RoleMenuAccess.builder()
                                .menu(menu)
                                .canView(true)
                                .canCreate(true)
                                .canEdit(true)
                                .canDelete(true)
                                .build();
                        map.put(menu.getId(), rma);
                    });
            return map;
        }

        Map<Long, RoleMenuAccess> map = new HashMap<>();
        Set<Long> roleIds = userDetails.getUser().getRoles()
                .stream().map(Role::getId).collect(Collectors.toSet());

        for (Long roleId : roleIds) {
            roleMenuAccessRepository.findViewableByRoleId(roleId)
                    .forEach(rma -> {
                        // If multiple roles grant access, merge with most-permissive wins
                        map.merge(rma.getMenu().getId(), rma, (existing, incoming) ->
                                RoleMenuAccess.builder()
                                        .menu(existing.getMenu())
                                        .canView(existing.isCanView()   || incoming.isCanView())
                                        .canCreate(existing.isCanCreate() || incoming.isCanCreate())
                                        .canEdit(existing.isCanEdit()   || incoming.isCanEdit())
                                        .canDelete(existing.isCanDelete() || incoming.isCanDelete())
                                        .build()
                        );
                    });
        }
        return map;
    }

    // ── Change password ───────────────────────────────────────────────────────

    /**
     * Changes the password for a user by their ID.
     * Called by MenuApiController /openApi/menus/change-password.
     *
     * @param userId      — the user's database ID
     * @param newPassword — plain-text new password (validation done in controller)
     */
    @Transactional
    public void changeUserPassword(Long userId, String newPassword) {
        var user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user #{} via menu API", userId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isVisible(AppMenu menu,
                               Set<Long>   explicitMenuIds,
                               Set<String> userAuthorities) {
        // Path 1: explicit role-menu grant
        if (explicitMenuIds.contains(menu.getId())) return true;

        // Path 2: requiredPermission fallback
        String rp = menu.getRequiredPermission();
        if (rp != null && !rp.isBlank() && userAuthorities.contains(rp)) return true;

        // MODULE / GROUP with no permission restriction → always visible
        // (JS hides empty containers automatically)
        String type = menu.getMenuType();
        return ("MODULE".equals(type) || "GROUP".equals(type))
                && (rp == null || rp.isBlank());
    }
}
