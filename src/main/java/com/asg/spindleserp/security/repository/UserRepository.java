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

/**
 * UserRepository
 *
 * Merges the uploaded version with the existing project repository.
 * Key additions over the uploaded file:
 *   • findByIdentifierWithRolesAndPermissions — single query for login
 *   • findByEmailWithRolesAndPermissions / findByPhoneWithRolesAndPermissions
 *     (already in project; also kept separately for explicit call sites)
 *   • existsByUsernameAndIdNot / existsByEmailAndIdNot — proper self-exclude
 *     on update (replaces the custom JPQL in the uploaded version)
 *   • JpaSpecificationExecutor for dynamic filtering
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    // ── Login loading  (JOIN FETCH prevents N+1 at login) ─────────────────────
    // Single query tries all three identifiers at once; used by UserDetailsServiceImpl.

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.deleted = false
          AND (u.username = :identifier
            OR u.email    = :identifier
            OR u.phone    = :identifier)
        """)
    Optional<User> findByIdentifierWithRolesAndPermissions(
            @Param("identifier") String identifier);

    // Kept separately so UserDetailsServiceImpl can chain .or() calls:

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.username = :id AND u.deleted = false
        """)
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("id") String id);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.email = :id AND u.deleted = false
        """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("id") String id);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.phone = :id AND u.deleted = false
        """)
    Optional<User> findByPhoneWithRolesAndPermissions(@Param("id") String id);

    // ── Simple lookups (management screens) ────────────────────────────────────

    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneAndDeletedFalse(String phone);
    Optional<User> findByIdAndDeletedFalse(Long id);

    // ── Existence checks ───────────────────────────────────────────────────────
    // Used in createUser() — no self-exclude needed.

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // Used in updateUser() — must exclude the user being updated.

    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    // ── Eager roles for management screens ────────────────────────────────────

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    // ── Lists ──────────────────────────────────────────────────────────────────

    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();
    List<User> findAllByEnabledTrueAndDeletedFalse();
    List<User> findByOrganizationIdAndDeletedFalse(Long orgId);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.roles
        WHERE u.deleted = false
          AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY u.createdAt DESC
        """)
    List<User> searchActive(@Param("q") String q);

    // ── Counts ─────────────────────────────────────────────────────────────────

    long countByDeletedFalse();
    long countByOrganizationIdAndDeletedFalse(Long orgId);
    long countByOrganizationIdAndDeletedFalseAndEnabledTrue(Long orgId);

    // ── Last login update ──────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :ts WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username,
                         @Param("ts") LocalDateTime ts);
}
