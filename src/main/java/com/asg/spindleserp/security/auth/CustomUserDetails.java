package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.entity.Permission;
import com.asg.spindleserp.security.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the User entity for Spring Security.
 *
 * Authorities are built ONCE at login from Role → Permission graph
 * and stored inside the Authentication object (held in the HTTP session).
 * This means ZERO database hits per request for permission checks.
 *
 * Each authority string is the Permission.name value (e.g. "purchase.order.view").
 * Role names are also added as "ROLE_XYZ" for @PreAuthorize("hasRole(...)") support.
 */
public class CustomUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    // Expose the full entity for menu-building and other UI needs
    @Getter
    private final User user;

    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    private static Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> auths = new LinkedHashSet<>();

        for (var role : user.getRoles()) {
            // Add ROLE_XXX so @PreAuthorize("hasRole('ADMIN')") works
            auths.add(new SimpleGrantedAuthority("ROLE_" + role.getName()
                    .replace("ROLE_", "")));  // avoid double prefix

            // Add every permission name from this role
            for (Permission perm : role.getPermissions()) {
                if (perm.isActive()) {
                    auths.add(new SimpleGrantedAuthority(perm.getName()));
                }
            }
        }
        return Collections.unmodifiableSet(auths);
    }

    // ── Convenience helpers used in controllers / Thymeleaf ──────────────

    public Long getUserId()         { return user.getId(); }
    public Long getOrganizationId() { return user.getOrganization().getId(); }
    public String getFullName()     { return user.getFullName(); }

    public boolean isSuperAdmin() {
        return user.getRoles().stream()
                .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName())
                            || "SUPER_ADMIN".equals(r.getName()));
    }

    public boolean hasPermission(String permissionName) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(permissionName));
    }

    // ── UserDetails contract ──────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public String  getPassword()                   { return user.getPassword(); }
    @Override public String  getUsername()                   { return user.getUsername(); }
    @Override public boolean isEnabled()                     { return user.isEnabled(); }
    @Override public boolean isAccountNonExpired()           { return user.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked()            { return user.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired()       { return user.isCredentialsNonExpired(); }
}
