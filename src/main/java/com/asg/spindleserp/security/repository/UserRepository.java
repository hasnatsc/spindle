package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    // ── Security loading (JOIN FETCH prevents N+1 on login) ──────────────

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.username = :identifier
          AND u.deleted = false
    """)
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("identifier") String identifier);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.email = :identifier
          AND u.deleted = false
    """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("identifier") String identifier);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions p
        WHERE u.phone = :identifier
          AND u.deleted = false
    """)
    Optional<User> findByPhoneWithRolesAndPermissions(@Param("identifier") String identifier);

    // ── Simple lookups (management screens) ──────────────────────────────

    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneAndDeletedFalse(String phone);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    List<User> findByOrganizationIdAndDeletedFalse(Long orgId);
    long       countByOrganizationIdAndDeletedFalse(Long orgId);
    long       countByOrganizationIdAndDeletedFalseAndEnabledTrue(Long orgId);
}
