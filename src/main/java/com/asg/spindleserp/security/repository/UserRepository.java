package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // ── Login ─────────────────────────────────────────────────────────────

    /** Multi-field login (username OR email OR phone). Loads roles+permissions. */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.deleted = false
          AND (u.username = :id OR u.email = :id OR u.phone = :id)
        """)
    Optional<User> findByIdentifierWithRolesAndPermissions(@Param("id") String identifier);

    // ── Context loading ───────────────────────────────────────────────────

    /**
     * Loads user WITH the four allowed-scope collections.
     * Used by LoginSuccessHandler and UserContextController.switch*().
     * Single query with four LEFT JOINs — avoids N+1 on the sets.
     *
     * NOTE: Hibernate may warn about the multi-bag fetch for roles when
     * combined with these joins. If that happens, split into two queries
     * (one for roles, one for scopes) or use Set<> everywhere (which you do).
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.organizations
        LEFT JOIN FETCH u.allowedBusinessUnits bu
        LEFT JOIN FETCH bu.organization
        LEFT JOIN FETCH u.allowedCostCenters cc
        LEFT JOIN FETCH cc.businessUnit
        LEFT JOIN FETCH u.allowedWarehouses wh
        LEFT JOIN FETCH wh.businessUnit
        WHERE u.username = :username AND u.deleted = false
        """)
    Optional<User> findByUsernameWithAllContext(@Param("username") String username);

    // ── Management lookups ────────────────────────────────────────────────

    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneAndDeletedFalse(String phone);
    Optional<User> findByIdAndDeletedFalse(Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions
        WHERE u.username = :username AND u.deleted = false
        """)
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    // ── Uniqueness checks ──────────────────────────────────────────────────

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    // ── Lists ─────────────────────────────────────────────────────────────

    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();
    List<User> findAllByEnabledTrueAndDeletedFalse();
    List<User> findByOrganizationIdAndDeletedFalse(Long orgId);

    @Query("""
        SELECT u FROM User u LEFT JOIN FETCH u.roles
        WHERE u.deleted = false
          AND (LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(u.email)    LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY u.createdAt DESC
        """)
    List<User> searchActive(@Param("q") String q);

    // ── Counts ────────────────────────────────────────────────────────────

    long countByDeletedFalse();
    long countByOrganizationIdAndDeletedFalse(Long orgId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId AND u.deleted = false")
    long countByRoleId(@Param("roleId") Long roleId);

    // ── Audit ─────────────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :ts WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("ts") LocalDateTime ts);
}
