package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.entity.Permission;
import com.asg.spindleserp.security.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the User JPA entity for Spring Security.
 *
 * Implements Serializable because Spring Session JDBC serializes the entire
 * Authentication object (which contains this) into the session store.
 *
 * Authorities are built ONCE at login from the Role → Permission graph
 * (already EAGER-loaded). Zero DB hits per subsequent request.
 *
 * NOTE: User.java now has only SINGLE boolean fields (enabled, accountNonLocked,
 * accountNonExpired, credentialsNonExpired) without the duplicate "is" prefixes.
 * Lombok generates isEnabled(), isAccountNonLocked(), etc. automatically.
 * The UserDetails method calls below match those generated getters.
 */
public class CustomUserDetails implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final User user;

    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user        = user;
        this.authorities = buildAuthorities(user);
    }

    private static Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> auths = new LinkedHashSet<>();
        for (var role : user.getRoles()) {
            // ROLE_ prefix for hasRole() / hasAnyRole() checks
            auths.add(new SimpleGrantedAuthority(
                "ROLE_" + role.getName().replace("ROLE_", "")));
            // Permission names for hasAuthority() / @PreAuthorize checks
            for (Permission perm : role.getPermissions()) {
                if (perm.isActive()) {
                    auths.add(new SimpleGrantedAuthority(perm.getName()));
                }
            }
        }
        return Collections.unmodifiableSet(auths);
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public Long   getUserId()         { return user.getId(); }
    public Long   getOrganizationId() { return user.getOrganization().getId(); }
    public String getOrganizationName(){ return user.getOrganization().getName(); }
    public String getFullName()       { return user.getFullName(); }

    public boolean isSuperAdmin() {
        return user.getRoles().stream()
                .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName())
                            || "SUPER_ADMIN".equals(r.getName()));
    }

    public boolean hasPermission(String permissionName) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(permissionName));
    }

    // ── UserDetails contract ──────────────────────────────────────────────────
    // These delegate to the single-boolean fields on User.
    // Lombok on User generates: isEnabled(), isAccountNonLocked(), etc.

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String  getPassword()             { return user.getPassword(); }
    @Override public String  getUsername()             { return user.getUsername(); }

    // ✅ These now match the SINGLE fields on User (no duplicate ambiguity)
    @Override public boolean isEnabled()               { return user.isEnabled(); }
    @Override public boolean isAccountNonExpired()     { return user.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked()      { return user.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired() { return user.isCredentialsNonExpired(); }
}
