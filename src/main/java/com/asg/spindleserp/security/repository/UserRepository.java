package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    // ── Login ─────────────────────────────────────────────────────────────────
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.deleted = false
          AND (u.username = :identifier OR u.email = :identifier OR u.phone = :identifier)
        """)
    Optional<User> findByIdentifierWithRolesAndPermissions(@Param("identifier") String identifier);

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

    // ── Management lookups ─────────────────────────────────────────────────────
    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneAndDeletedFalse(String phone);
    Optional<User> findByIdAndDeletedFalse(Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    // ── Uniqueness checks ──────────────────────────────────────────────────────
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    // ── Lists ─────────────────────────────────────────────────────────────────
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

    // ── Counts ────────────────────────────────────────────────────────────────
    long countByDeletedFalse();
    long countByOrganizationIdAndDeletedFalse(Long orgId);

    /** Used by RoleServiceImpl to guard deletion of roles still in use */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId AND u.deleted = false")
    long countByRoleId(@Param("roleId") Long roleId);

    // ── Last login ────────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :ts WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("ts") LocalDateTime ts);
}
