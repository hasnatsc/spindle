package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RoleRepository
 *
 * Provides all role lookups needed by:
 *   UserController.allRoles()         → findAllActiveWithPermissions()
 *   UserServiceImpl.resolveRoles()    → findAllById() (from JpaRepository)
 *   SecurityDataInitializer           → findByName()
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    /** For UserController /roles/all — returns active roles with permissions. */
    @Query("""
        SELECT DISTINCT r FROM Role r
        LEFT JOIN FETCH r.permissions p
        WHERE r.active = true
        ORDER BY r.name
        """)
    List<Role> findAllActiveWithPermissions();

    /** All active roles (no permission fetch — for dropdowns). */
    List<Role> findAllByActiveTrueOrderByName();
}
