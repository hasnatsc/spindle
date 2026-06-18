package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.UserContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserContextRepository
 *
 * Manages the user_context table — one row per user.
 * The row stores the user's *default working context*:
 *   defaultOrganizationId / defaultBusinessUnitId /
 *   defaultCostCenterId   / defaultWarehouseId
 *
 * Any module can call ContextProvider.currentContext() to get these
 * values from a @SessionScope bean — zero DB hits after login.
 *
 * Table DDL (run once):
 *   CREATE TABLE IF NOT EXISTS user_context (
 *       user_id              BIGINT PRIMARY KEY
 *                            REFERENCES sec_users(id) ON DELETE CASCADE,
 *       organization_id      BIGINT,
 *       business_unit_id     BIGINT,
 *       cost_center_id       BIGINT,
 *       warehouse_id         BIGINT,
 *       approval_default_view              VARCHAR(20),
 *       approval_desktop_notification      BOOLEAN,
 *       approval_email_enabled             BOOLEAN,
 *       approval_push_enabled              BOOLEAN,
 *       approval_sms_enabled               BOOLEAN,
 *       approval_whatsapp_enabled          BOOLEAN,
 *       approval_sound_enabled             BOOLEAN,
 *       approval_notification_frequency    VARCHAR(20),
 *       approval_refresh_interval          INTEGER,
 *       show_approval_badge                BOOLEAN,
 *       last_viewed_notification_id        BIGINT
 *   );
 */
@Repository
public interface UserContextRepository extends JpaRepository<UserContext, Long> {

    Optional<UserContext> findByUserId(Long userId);
}
