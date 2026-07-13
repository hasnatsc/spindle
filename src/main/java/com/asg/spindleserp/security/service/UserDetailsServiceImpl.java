package com.asg.spindleserp.security.service;

import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.auth.WebSecurityUtils;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsServiceImpl — loads a user by username, email OR phone.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * ★ THE FIX: this class no longer throws DisabledException / LockedException.
 * ══════════════════════════════════════════════════════════════════════════
 *
 * It used to:
 *
 *     if (!user.isEnabled())          throw new DisabledException("…");
 *     if (!user.isAccountNonLocked()) throw new LockedException("…");
 *
 * That looks completely reasonable, and it silently did not work.
 *
 * DaoAuthenticationProvider.retrieveUser() wraps anything coming out of
 * loadUserByUsername() that is not a UsernameNotFoundException:
 *
 *     try {
 *         UserDetails loadedUser = getUserDetailsService().loadUserByUsername(username);
 *         …
 *     }
 *     catch (UsernameNotFoundException ex) { mitigateAgainstTimingAttack(…); throw ex; }
 *     catch (InternalAuthenticationServiceException ex) { throw ex; }
 *     catch (Exception ex) {
 *         throw new InternalAuthenticationServiceException(ex.getMessage(), ex);   // ← here
 *     }
 *
 * So the DisabledException thrown above never reached LoginFailureHandler AS a
 * DisabledException. It arrived as an InternalAuthenticationServiceException.
 * And LoginFailureHandler tested:
 *
 *     } else if (exception instanceof DisabledException) {
 *         setDefaultFailureUrl("/login?disabled");
 *
 * …which was therefore ALWAYS false. /login?disabled and /login?locked were
 * unreachable. Every disabled user, and every locked user, was told "Invalid
 * username / email / phone or password" and had no idea why their correct
 * password stopped working. The LoginController even had the @RequestParam and
 * the alert message for both cases, wired and ready, and neither could ever be
 * triggered. Two dead code paths, sitting there looking like they worked.
 *
 * THE CORRECT MECHANISM (which Spring gives us for free):
 *
 *   AbstractUserDetailsAuthenticationProvider.authenticate() runs
 *   preAuthenticationChecks (DefaultPreAuthenticationChecks) on the UserDetails
 *   AFTER retrieveUser() has returned — i.e. OUTSIDE the try/catch that does the
 *   wrapping — and BEFORE the password comparison:
 *
 *       if (!user.isAccountNonLocked())      throw new LockedException(…);
 *       if (!user.isEnabled())               throw new DisabledException(…);
 *       if (!user.isAccountNonExpired())     throw new AccountExpiredException(…);
 *
 *   CustomUserDetails already delegates all four flags to the User entity. So
 *   the ONLY thing needed is to STOP throwing by hand and let those checks run.
 *   They are not wrapped. LoginFailureHandler now sees the real types, and
 *   /login?disabled and /login?locked work for the first time.
 *
 * WHAT STAYS: the soft-delete check. A deleted account must look like it never
 * existed (UsernameNotFoundException), not like a disabled one — that is the one
 * case where the "wrap" behaviour is actually correct, because
 * UsernameNotFoundException is explicitly excluded from wrapping AND gets
 * Spring's built-in timing-attack mitigation applied to it.
 *
 * ── Also ──────────────────────────────────────────────────────────────────
 * The submitted identifier is now sanitised before it is logged. It is raw user
 * input, and CR/LF in it forges log lines (CWE-117).
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        String safeId = WebSecurityUtils.sanitizeForLog(identifier);   // ✅ CWE-117

        // Single optimised query — tries username | email | phone.
        User user = userRepository
                .findByIdentifierWithRolesAndPermissions(identifier)
                .orElseThrow(() -> {
                    log.warn("Login failed — no user for identifier: {}", safeId);
                    return new UsernameNotFoundException("Invalid username or password.");
                });

        // ✅ KEPT — a soft-deleted account must be indistinguishable from one that
        //    never existed. UsernameNotFoundException is the one exception type
        //    DaoAuthenticationProvider does NOT wrap, and it is the one it applies
        //    timing-attack mitigation to. Correct on both counts.
        if (user.isDeleted()) {
            log.warn("Login failed — deleted account: {}", safeId);
            throw new UsernameNotFoundException("Account does not exist.");
        }

        // ✅ REMOVED — the DisabledException / LockedException throws that used to
        //    live here. They were silently wrapped into
        //    InternalAuthenticationServiceException and could never be recognised
        //    downstream. Spring's DefaultPreAuthenticationChecks now throws them,
        //    unwrapped, from the correct place, using the flags CustomUserDetails
        //    already exposes:
        //
        //        isEnabled()               → user.enabled
        //        isAccountNonLocked()      → user.accountNonLocked
        //        isAccountNonExpired()     → user.accountNonExpired
        //        isCredentialsNonExpired() → user.credentialsNonExpired
        //
        //    Nothing is lost — the SAME checks run, from the SAME columns, and now
        //    the failure handler can actually see the result.

        if (log.isDebugEnabled()) {
            log.debug("Loaded user='{}' roles={} enabled={} nonLocked={}",
                    WebSecurityUtils.sanitizeForLog(user.getUsername()),
                    user.getRoles().size(), user.isEnabled(), user.isAccountNonLocked());
        }

        return new CustomUserDetails(user);
    }
}
