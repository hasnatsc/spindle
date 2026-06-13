package com.asg.spindleserp.security.service;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.entity.AppMenu;
import com.asg.spindleserp.security.entity.RoleMenuAccess;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.repository.AppMenuRepository;
import com.asg.spindleserp.security.repository.RoleMenuAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the navigation menu tree for the currently logged-in user.
 *
 * Two access paths:
 *   Path 1 (RoleMenuAccess): menu has explicit canView=true entry for the user's role
 *   Path 2 (requiredPermission fallback): menu.requiredPermission is in user's authorities
 *
 * SUPER_ADMIN sees all menus.
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final AppMenuRepository        menuRepository;
    private final RoleMenuAccessRepository roleMenuAccessRepository;

    /**
     * Returns the flat list of menus the user can see, ordered by displayOrder.
     * The Thymeleaf template uses parentId to build the tree structure.
     */
    @Transactional(readOnly = true)
    public List<AppMenu> getVisibleMenus(CustomUserDetails userDetails) {

        // SUPER_ADMIN sees everything
        if (userDetails.isSuperAdmin()) {
            return menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
        }

        // Collect all role IDs the user has
        Set<Long> roleIds = userDetails.getUser().getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        // Collect menu IDs visible via explicit RoleMenuAccess
        Set<Long> allowedMenuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            roleMenuAccessRepository.findViewableByRoleId(roleId).stream()
                    .map(rma -> rma.getMenu().getId())
                    .forEach(allowedMenuIds::add);
        }

        // Collect user's permission names for fallback check
        Set<String> userPermissions = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        // Get all active menus and filter
        List<AppMenu> allMenus =
                menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

        List<AppMenu> visible = allMenus.stream().filter(menu -> {
            // Path 1: explicit role-menu access
            if (allowedMenuIds.contains(menu.getId())) return true;
            // Path 2: requiredPermission fallback
            if (menu.getRequiredPermission() != null
                    && !menu.getRequiredPermission().isBlank()
                    && userPermissions.contains(menu.getRequiredPermission())) {
                return true;
            }
            // MODULE and GROUP entries with no permission set are included
            // only if they have at least one visible child (handled in Thymeleaf)
            return ("MODULE".equals(menu.getMenuType()) || "GROUP".equals(menu.getMenuType()))
                    && menu.getRequiredPermission() == null;
        }).collect(Collectors.toList());

        return visible;
    }

    /**
     * Returns a map of menuId → RoleMenuAccess for the user's roles.
     * Used in Thymeleaf to check canCreate / canEdit / canDelete per menu.
     */
    @Transactional(readOnly = true)
    public Map<Long, RoleMenuAccess> getMenuAccessMap(CustomUserDetails userDetails) {
        Map<Long, RoleMenuAccess> map = new HashMap<>();
        for (Role role : userDetails.getUser().getRoles()) {
            roleMenuAccessRepository.findViewableByRoleId(role.getId())
                    .forEach(rma -> map.merge(rma.getMenu().getId(), rma,
                            // If multiple roles grant access, take the most permissive
                            (existing, newer) -> {
                                if (newer.isCanCreate()) existing.setCanCreate(true);
                                if (newer.isCanEdit())   existing.setCanEdit(true);
                                if (newer.isCanDelete()) existing.setCanDelete(true);
                                return existing;
                            }));
        }
        return map;
    }
}
