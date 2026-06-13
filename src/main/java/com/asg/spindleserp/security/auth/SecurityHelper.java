package com.asg.spindleserp.security.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Static utility — use these in controllers and services to get
 * the current user without injecting a bean.
 *
 * Example:
 *   Long orgId = SecurityHelper.currentOrgId()
 *                    .orElseThrow(() -> new IllegalStateException("Not authenticated"));
 */
public final class SecurityHelper {

    private SecurityHelper() {}

    public static Optional<CustomUserDetails> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof CustomUserDetails ud) return Optional.of(ud);
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(CustomUserDetails::getUserId);
    }

    public static Optional<Long> currentOrgId() {
        return currentUser().map(CustomUserDetails::getOrganizationId);
    }

    public static Optional<String> currentUsername() {
        return currentUser().map(CustomUserDetails::getUsername);
    }

    public static boolean isSuperAdmin() {
        return currentUser().map(CustomUserDetails::isSuperAdmin).orElse(false);
    }

    public static boolean hasPermission(String permissionName) {
        return currentUser().map(u -> u.hasPermission(permissionName)).orElse(false);
    }

    /**
     * Convenience — throws if not authenticated.
     * Use in service methods that require a user context.
     */
    public static CustomUserDetails requireCurrentUser() {
        return currentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }

    public static Long requireOrgId() {
        return requireCurrentUser().getOrganizationId();
    }
}
