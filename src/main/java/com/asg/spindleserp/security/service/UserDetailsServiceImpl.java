package com.asg.spindleserp.security.service;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsServiceImpl
 *
 * Loads a user by username, email, OR phone (whichever was entered).
 * Uses a single optimized query: findByIdentifierWithRolesAndPermissions.
 * Falls back to the three separate queries if the single one is not preferred.
 *
 * Fix over uploaded version:
 *   user.isAccountLocked() → NOT a method on the User entity.
 *   User has `accountNonLocked` (boolean); Lombok generates isAccountNonLocked().
 *   The correct check is:  !user.isAccountNonLocked()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        // Single optimized query: tries username | email | phone
        User user = userRepository
                .findByIdentifierWithRolesAndPermissions(identifier)
                .orElseThrow(() -> {
                    log.warn("Login failed — no user found for identifier: {}", identifier);
                    return new UsernameNotFoundException("Invalid username or password.");
                });

        // Soft-delete check
        if (user.isDeleted()) {
            log.warn("Login failed — deleted account: {}", identifier);
            throw new UsernameNotFoundException("Account does not exist.");
        }

        // Disabled check
        if (!user.isEnabled()) {
            log.warn("Login failed — disabled account: {}", identifier);
            throw new DisabledException("Your account has been disabled. Contact your administrator.");
        }

        // ✅ FIX: was user.isAccountLocked() which does not exist on User.
        // Correct method is !user.isAccountNonLocked() (Lombok from 'accountNonLocked' field).
        if (!user.isAccountNonLocked()) {
            log.warn("Login failed — locked account: {}", identifier);
            throw new LockedException("Your account is locked. Contact your administrator.");
        }

        log.debug("Login OK — user='{}' roles={}", user.getUsername(), user.getRoles().size());
        return new CustomUserDetails(user);
    }
}
