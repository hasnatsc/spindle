package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.RoleMenuAccess;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {

    /** Navigation: menus the role can VIEW — JOIN FETCH avoids N+1 */
    @Query("""
        SELECT rma FROM RoleMenuAccess rma
        JOIN FETCH rma.menu m
        WHERE rma.role.id = :roleId
          AND rma.canView = true
          AND m.active    = true
          AND m.deleted   = false
        ORDER BY m.displayOrder ASC
        """)
    List<RoleMenuAccess> findViewableByRoleId(@Param("roleId") Long roleId);

    /** Management: ALL access rows for a role (used by matrix screen) */
    @Query("""
        SELECT rma FROM RoleMenuAccess rma
        JOIN FETCH rma.menu m
        WHERE rma.role.id = :roleId
        ORDER BY m.displayOrder ASC
        """)
    List<RoleMenuAccess> findAllByRoleId(@Param("roleId") Long roleId);

    /** All access rows for a specific menu (for menu-management screen) */
    @Query("""
        SELECT rma FROM RoleMenuAccess rma
        JOIN FETCH rma.role r
        WHERE rma.menu.id = :menuId
        """)
    List<RoleMenuAccess> findAllByMenuId(@Param("menuId") Long menuId);

    Optional<RoleMenuAccess> findByRoleIdAndMenuId(Long roleId, Long menuId);

    boolean existsByRoleIdAndMenuId(Long roleId, Long menuId);

    /** Bulk-delete: called before re-saving full access set for a role */
    @Modifying
    @Query("DELETE FROM RoleMenuAccess rma WHERE rma.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
