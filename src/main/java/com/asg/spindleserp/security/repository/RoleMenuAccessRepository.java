package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.RoleMenuAccess;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {

    List<RoleMenuAccess> findByRoleId(Long roleId);

    Optional<RoleMenuAccess> findByRoleIdAndMenuId(Long roleId, Long menuId);

    boolean existsByRoleIdAndMenuId(Long roleId, Long menuId);

    @Modifying
    @Query("DELETE FROM RoleMenuAccess rma WHERE rma.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /** All menus a role can VIEW — used for dynamic navigation building */
    @Query("""
        SELECT rma FROM RoleMenuAccess rma
        JOIN FETCH rma.menu m
        WHERE rma.role.id = :roleId
          AND rma.canView = true
          AND m.active = true
          AND m.deleted = false
        ORDER BY m.displayOrder
    """)
    List<RoleMenuAccess> findViewableByRoleId(@Param("roleId") Long roleId);
}
