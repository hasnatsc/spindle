package com.asg.spindleserp.security.menu;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.entity.AppMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MenuApiController
 *
 * Serves the navigation menu for the dynamic top-menu JS in topMenu.html.
 *
 * URL prefix : /openApi/menus
 * All endpoints are PUBLIC (permitted in SecurityConfig without auth)
 * because the menu JS runs before the session is verified by Spring Security.
 * The actual menu items returned are filtered per the logged-in user's roles.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * GET /openApi/menus/user-menu
 * ═══════════════════════════════════════════════════════════════════════
 * Returns the flat list of visible AppMenu items for the current user.
 * The topMenu.html JS transforms them into a tree using parentMenuId.
 *
 * Response shape:
 * {
 *   "success": true,
 *   "menus": {
 *     "items": [
 *       {
 *         "id":           1,
 *         "parentMenuId": null,          ← top-level MODULE
 *         "menuName":     "Security",
 *         "menuUrl":      null,
 *         "icon":         "fa fa-lock",
 *         "displayOrder": 10,
 *         "menuType":     "MODULE",
 *         "target":       "_self"
 *       },
 *       {
 *         "id":           5,
 *         "parentMenuId": 1,             ← child of Security
 *         "menuName":     "User Management",
 *         "menuUrl":      "/users",
 *         "icon":         "fa fa-users",
 *         "displayOrder": 1,
 *         "menuType":     "LEAF",
 *         "target":       "_self"
 *       }
 *     ]
 *   }
 * }
 *
 * ═══════════════════════════════════════════════════════════════════════
 * POST /openApi/menus/change-password
 * ═══════════════════════════════════════════════════════════════════════
 * Called by the topMenuHeader.html Change Password SweetAlert2 form.
 * Body: { "password": "newPassword123" }
 */
@Slf4j
@RestController
@RequestMapping("/openApi/menus")
@RequiredArgsConstructor
public class MenuApiController {

    private final MenuService menuService;

    // ── User-menu endpoint ────────────────────────────────────────────────────

    @GetMapping("/user-menu")
    public Map<String, Object> userMenu(
            @AuthenticationPrincipal CustomUserDetails principal) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            if (principal == null) {
                response.put("success", false);
                response.put("message", "Not authenticated.");
                return response;
            }

            List<AppMenu> visible = menuService.getVisibleMenus(principal);

            // Build flat item list — JS does the tree-building in the browser
            List<Map<String, Object>> items = new ArrayList<>();
            for (AppMenu menu : visible) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",           menu.getId());
                // parentMenuId: the JS uses this field name to build the tree
                item.put("parentMenuId", menu.getParentId());
                item.put("menuName",     menu.getMenuName());
                item.put("menuUrl",      menu.getMenuUrl());
                item.put("icon",         menu.getIcon() != null ? menu.getIcon() : "fa fa-circle");
                item.put("displayOrder", menu.getDisplayOrder());
                item.put("menuType",     menu.getMenuType());
                item.put("target",       menu.getTarget() != null ? menu.getTarget() : "_self");
                items.add(item);
            }

            Map<String, Object> menus = new LinkedHashMap<>();
            menus.put("items", items);

            response.put("success", true);
            response.put("menus",   menus);

        } catch (Exception e) {
            log.error("[MenuApi] user-menu failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to load menu: " + e.getMessage());
        }

        return response;
    }

    // ── Change-password endpoint ──────────────────────────────────────────────

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestBody  Map<String, String>   body,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (principal == null) {
                response.put("success", false);
                response.put("message", "Not authenticated.");
                return response;
            }

            String newPassword = body.get("password");

            if (newPassword == null || newPassword.isBlank()) {
                response.put("success", false);
                response.put("message", "Password is required.");
                return response;
            }

            if (newPassword.length() < 8) {
                response.put("success", false);
                response.put("message", "Password must be at least 8 characters.");
                return response;
            }

            menuService.changeUserPassword(principal.getUserId(), newPassword);

            response.put("success", true);
            response.put("message", "Password changed successfully.");

        } catch (Exception e) {
            log.error("[MenuApi] change-password failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }
}
