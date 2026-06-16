package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository
 *
 * All methods are soft-delete-aware (deleted = false guard).
 * Methods used by UserServiceImpl and UserDetailsServiceImpl
 * are fully included here.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Login loading (JOIN FETCH avoids N+1 at authentication time) ──────────

    /**
     * Single-query login lookup: tries username | email | phone.
     * Used by UserDetailsServiceImpl.
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.deleted = false
          AND (u.username = :id OR u.email = :id OR u.phone = :id)
        """)
    Optional<User> findByIdentifierWithRolesAndPermissions(@Param("id") String identifier);

    /** Explicit lookups used by UserDetailsServiceImpl .or() chain. */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions
        WHERE u.username = :id AND u.deleted = false
        """)
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("id") String id);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions
        WHERE u.email = :id AND u.deleted = false
        """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("id") String id);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions
        WHERE u.phone = :id AND u.deleted = false
        """)
    Optional<User> findByPhoneWithRolesAndPermissions(@Param("id") String id);

    // ── Management lookups ────────────────────────────────────────────────────

    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneAndDeletedFalse(String phone);
    Optional<User> findByIdAndDeletedFalse(Long id);

    /** Load with roles for CRUD operations (not for auth — no permission fetch). */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    // ── Uniqueness checks ─────────────────────────────────────────────────────

    // For CREATE (no self-exclude needed)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // For UPDATE (exclude self by id)
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    // ── Lists ─────────────────────────────────────────────────────────────────

    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();
    List<User> findAllByEnabledTrueAndDeletedFalse();

    @Query("""
        SELECT u FROM User u LEFT JOIN FETCH u.roles
        WHERE u.deleted = false
          AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY u.createdAt DESC
        """)
    List<User> searchActive(@Param("q") String q);

    // ── Counts ────────────────────────────────────────────────────────────────

    long countByDeletedFalse();

    // ── Last-login update ─────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :ts WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("ts") LocalDateTime ts);
}
