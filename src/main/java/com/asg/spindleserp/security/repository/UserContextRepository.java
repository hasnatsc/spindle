package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.UserContext;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContextRepository extends JpaRepository<UserContext, Long> {

    /**
     * Full eager load — used at login and after switch.
     * Joins org/BU/CC/WH in one query so loadContext() does a single DB round-trip.
     */
    @Query("""
        SELECT c FROM UserContext c
        LEFT JOIN FETCH c.organization
        LEFT JOIN FETCH c.businessUnit
        LEFT JOIN FETCH c.costCenter
        LEFT JOIN FETCH c.warehouse
        WHERE c.user.id = :userId
        """)
    Optional<UserContext> findByUserIdEager(@Param("userId") Long userId);

    /** Pessimistic write lock for switch operations — prevents race conditions. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM UserContext c WHERE c.user.id = :userId")
    Optional<UserContext> findByUserIdForUpdate(@Param("userId") Long userId);

    // ── Approval preference JPQL updates ─────────────────────────────────

    @Modifying
    @Query("""
        UPDATE UserContext c SET
            c.approvalNotificationFrequency = :freq,
            c.approvalEmailEnabled          = :email,
            c.approvalSmsEnabled            = :sms,
            c.approvalPushEnabled           = :push,
            c.approvalWhatsappEnabled       = :wa
        WHERE c.user.id = :userId
        """)
    int updateNotificationPrefs(
            @Param("userId") Long userId,
            @Param("freq")   String freq,
            @Param("email")  Boolean email,
            @Param("sms")    Boolean sms,
            @Param("push")   Boolean push,
            @Param("wa")     Boolean wa);

    @Modifying
    @Query("""
        UPDATE UserContext c SET
            c.approvalDefaultView         = :view,
            c.approvalRefreshInterval     = :interval,
            c.approvalSoundEnabled        = :sound,
            c.approvalDesktopNotification = :desktop,
            c.showApprovalBadge           = :badge
        WHERE c.user.id = :userId
        """)
    int updateDisplayPrefs(
            @Param("userId")   Long    userId,
            @Param("view")     String  view,
            @Param("interval") Integer interval,
            @Param("sound")    Boolean sound,
            @Param("desktop")  Boolean desktop,
            @Param("badge")    Boolean badge);

    @Modifying
    @Query("UPDATE UserContext c SET c.lastViewedNotificationId = :id WHERE c.user.id = :userId")
    int updateLastViewedNotification(@Param("userId") Long userId, @Param("id") Long id);

    // ── Broadcast helpers ─────────────────────────────────────────────────

    @Query("SELECT c.user.id FROM UserContext c WHERE c.organization.id = :orgId")
    List<Long> findUserIdsByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT c.user.id FROM UserContext c WHERE c.businessUnit.id = :buId")
    List<Long> findUserIdsByBusinessUnitId(@Param("buId") Long buId);

    @Query("SELECT c.user.id FROM UserContext c WHERE c.showApprovalBadge = true")
    List<Long> findUserIdsWithApprovalBadgeEnabled();

    @Query("SELECT c.user.id FROM UserContext c WHERE c.approvalEmailEnabled = true")
    List<Long> findUserIdsWithEmailEnabled();
}
