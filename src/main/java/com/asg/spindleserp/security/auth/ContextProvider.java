package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.organization.entity.UserContext;
import com.asg.spindleserp.organization.repository.UserContextRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;

/**
 * ContextProvider — session-scoped, serializable Spring bean.
 *
 * ══════════════════════════════════════════════════════════════════════
 * PURPOSE
 * ══════════════════════════════════════════════════════════════════════
 * Provides the current user's *default working context* (org / BU /
 * cost-center / warehouse) with ZERO database hits after the first
 * access per session.
 *
 * ══════════════════════════════════════════════════════════════════════
 * HOW IT WORKS
 * ══════════════════════════════════════════════════════════════════════
 * 1. The bean is @SessionScope — Spring creates one instance per HTTP
 *    session and stores it in the session (serialized by Spring Session JDBC).
 * 2. On the first call to getContext() within a session, the bean
 *    queries user_context once and caches the row.
 * 3. All subsequent calls within the same session return the cached value.
 * 4. After the user saves a new context via /users/context/save, the
 *    controller calls refresh() so the next read re-queries.
 *
 * ══════════════════════════════════════════════════════════════════════
 * USAGE IN SERVICES / CONTROLLERS
 * ══════════════════════════════════════════════════════════════════════
 *   @Autowired private ContextProvider contextProvider;
 *
 *   // --- In a controller ---
 *   Long orgId = contextProvider.getOrganizationId();
 *
 *   // --- In an entity builder (static pattern like ContextProvider.getOrgId()) ---
 *   // Inject ContextProvider via constructor or @Autowired in the service,
 *   // then use instance methods — do NOT use static calls.
 *
 *   // ✅ Correct pattern (service has @Autowired ContextProvider):
 *   Organization org = organizationRepository
 *       .getReferenceById(contextProvider.getOrganizationId());
 *
 * ══════════════════════════════════════════════════════════════════════
 * SERIALIZATION NOTE
 * ══════════════════════════════════════════════════════════════════════
 * Must implement Serializable because Spring Session JDBC serializes
 * the entire session (including all session-scoped beans) to the DB.
 * The repository fields are marked transient — they are re-injected by
 * Spring after deserialization via the bean lifecycle.
 */
@Component
@SessionScope
@Slf4j
public class ContextProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Transient — re-injected after deserialization
    private final transient UserContextRepository contextRepository;
    private final transient UserRepository        userRepository;

    // Cached context for this session
    private volatile UserContext cachedContext;

    public ContextProvider(UserContextRepository contextRepository,
                           UserRepository userRepository) {
        this.contextRepository = contextRepository;
        this.userRepository    = userRepository;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the full UserContext for the current user.
     * Loads from DB on first call; cached on subsequent calls.
     */
    public UserContext getContext() {
        if (cachedContext == null) {
            reload();
        }
        return cachedContext;
    }

    /** Default org ID — falls back to the user's primary org if not set. */
    public Long getOrganizationId() {
        UserContext ctx = getContext();
        if (ctx.getOrganizationId() != null) return ctx.getOrganizationId();
        // fallback: the org the user was created under
        return SecurityHelper.currentUser()
                .map(CustomUserDetails::getOrganizationId)
                .orElse(null);
    }

    public Long getBusinessUnitId() { return getContext().getBusinessUnitId(); }
    public Long getCostCenterId()   { return getContext().getCostCenterId();   }
    public Long getWarehouseId()    { return getContext().getWarehouseId();    }

    /**
     * Force a re-load from the database.
     * Call this after saving a new context so the session cache is fresh.
     */
    public void refresh() {
        cachedContext = null;
        log.debug("ContextProvider cache cleared for user={}",
                SecurityHelper.currentUsername().orElse("unknown"));
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void reload() {
        Long userId = SecurityHelper.currentUser()
                .map(CustomUserDetails::getUserId)
                .orElse(null);

        if (userId == null) {
            cachedContext = new UserContext();   // empty — unauthenticated
            return;
        }

        cachedContext = contextRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserContext empty = new UserContext();
                    empty.setUserId(userId);
                    return empty;
                });

        log.debug("ContextProvider loaded: userId={} org={} bu={} cc={} wh={}",
                userId,
                cachedContext.getOrganizationId(),
                cachedContext.getBusinessUnitId(),
                cachedContext.getCostCenterId(),
                cachedContext.getWarehouseId());
    }
}
