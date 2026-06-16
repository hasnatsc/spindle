package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.RoleMenuAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RoleMenuAccessRepository
 *
 * Manages the sec_mrole_menus join table.
 * Key method: findViewableByRoleId — used by MenuService to resolve
 * which menus a role can see (canView = true).
 */
@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {

    /**
     * All menu access entries where canView = true for a given role.
     * JOIN FETCH menu avoids N+1 in MenuService.getVisibleMenus().
     */
    @Query("""
        SELECT rma FROM RoleMenuAccess rma
        JOIN FETCH rma.menu m
        WHERE rma.role.id = :roleId
          AND rma.canView = true
          AND m.active  = true
          AND m.deleted = false
        ORDER BY m.displayOrder ASC
        """)
    List<RoleMenuAccess> findViewableByRoleId(@Param("roleId") Long roleId);

    /**
     * All access entries for a role (any canView value).
     * Used by getMenuAccessMap() to merge permissions.
     */
    @Query("""
        SELECT rma FROM RoleMenuAccess rma
        JOIN FETCH rma.menu
        WHERE rma.role.id = :roleId
        """)
    List<RoleMenuAccess> findAllByRoleId(@Param("roleId") Long roleId);

    /**
     * Look up a specific role ↔ menu access record.
     * Used by menu-management CRUD.
     */
    Optional<RoleMenuAccess> findByRoleIdAndMenuId(Long roleId, Long menuId);

    boolean existsByRoleIdAndMenuId(Long roleId, Long menuId);

    /** All access records for a menu (for menu-management UI). */
    List<RoleMenuAccess> findAllByMenuId(Long menuId);
}
