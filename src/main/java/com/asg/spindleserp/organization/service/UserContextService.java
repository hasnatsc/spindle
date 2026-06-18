package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.organization.entity.UserContext;
import com.asg.spindleserp.organization.repository.UserContextRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UserContextService
 *
 * Loads and persists the user_context row for the currently logged-in user.
 *
 * Usage:
 *   // In any controller or service:
 *   UserContext ctx = userContextService.currentContext();
 *   Long orgId = ctx.getOrganizationId();   // never null if set via UI
 *
 * The ContextProvider bean (session-scoped) wraps this service so that
 * the row is loaded once per session and cached in memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserContextRepository contextRepository;
    private final UserRepository        userRepository;

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Returns the UserContext for the given userId.
     * If no row exists yet, returns an empty (defaults-null) context.
     */
    @Transactional(readOnly = true)
    public UserContext loadByUserId(Long userId) {
        return contextRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserContext ctx = new UserContext();
                    ctx.setUserId(userId);
                    return ctx;   // not persisted until save() is called
                });
    }

    /**
     * Returns the UserContext for the currently logged-in user.
     * Throws if no authenticated user is present.
     */
    @Transactional(readOnly = true)
    public UserContext currentContext() {
        Long uid = SecurityHelper.requireCurrentUser().getUserId();
        return loadByUserId(uid);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Persists the default context fields for the currently logged-in user.
     * Creates the row if it doesn't exist; updates if it does.
     *
     * @param organizationId  may be null (clears the default)
     * @param businessUnitId  may be null
     * @param costCenterId    may be null
     * @param warehouseId     may be null
     */
    @Transactional
    public UserContext saveContext(Long organizationId,
                                   Long businessUnitId,
                                   Long costCenterId,
                                   Long warehouseId) {

        Long userId = SecurityHelper.requireCurrentUser().getUserId();

        UserContext ctx = contextRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserContext c = new UserContext();
                    c.setUserId(userId);
                    // link to the User entity (MapsId requires it for insert)
                    c.setUser(userRepository.getReferenceById(userId));
                    return c;
                });

        ctx.setOrganizationId(organizationId);
        ctx.setBusinessUnitId(businessUnitId);
        ctx.setCostCenterId(costCenterId);
        ctx.setWarehouseId(warehouseId);

        UserContext saved = contextRepository.save(ctx);
        log.info("UserContext saved: userId={} org={} bu={} cc={} wh={}",
                userId, organizationId, businessUnitId, costCenterId, warehouseId);
        return saved;
    }

    /**
     * Convenience — save context for a specific user (called by UserService
     * when updating a user's default context from the admin form).
     */
    @Transactional
    public UserContext saveContextForUser(Long userId,
                                          Long organizationId,
                                          Long businessUnitId,
                                          Long costCenterId,
                                          Long warehouseId) {

        UserContext ctx = contextRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserContext c = new UserContext();
                    c.setUserId(userId);
                    c.setUser(userRepository.getReferenceById(userId));
                    return c;
                });

        ctx.setOrganizationId(organizationId);
        ctx.setBusinessUnitId(businessUnitId);
        ctx.setCostCenterId(costCenterId);
        ctx.setWarehouseId(warehouseId);

        return contextRepository.save(ctx);
    }

    // ── Convenience getters ───────────────────────────────────────────────────

    /** Current user's default org ID or null */
    @Transactional(readOnly = true)
    public Long currentOrgId() {
        return Optional.ofNullable(currentContext().getOrganizationId())
                .orElse(SecurityHelper.requireCurrentUser().getOrganizationId());
    }

    /** Current user's default BU ID or null */
    @Transactional(readOnly = true)
    public Long currentBusinessUnitId() {
        return currentContext().getBusinessUnitId();
    }

    /** Current user's default CostCenter ID or null */
    @Transactional(readOnly = true)
    public Long currentCostCenterId() {
        return currentContext().getCostCenterId();
    }

    /** Current user's default Warehouse ID or null */
    @Transactional(readOnly = true)
    public Long currentWarehouseId() {
        return currentContext().getWarehouseId();
    }
}
