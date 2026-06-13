package com.asg.spindleserp.security.service;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a user by username, email, OR phone (whichever was entered in the login form).
 *
 * The @Transactional ensures that the lazy Role→Permission collection is loaded
 * within the same session before the entity is detached.
 * After this method returns, all authorities are cached in CustomUserDetails —
 * no further DB hits for permission checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        // Try username first, then email, then phone
        User user = userRepository
                .findByUsernameWithRolesAndPermissions(identifier)
                .or(() -> userRepository.findByEmailWithRolesAndPermissions(identifier))
                .or(() -> userRepository.findByPhoneWithRolesAndPermissions(identifier))
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No user found for: " + identifier));

        if (user.isDeleted()) {
            throw new UsernameNotFoundException("Account has been deleted: " + identifier);
        }

        log.debug("Loaded user '{}' with {} role(s)", user.getUsername(),
                user.getRoles().size());

        return new CustomUserDetails(user);
    }
}
