package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.RoleMenuAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {
    List<RoleMenuAccess> findByRoleId(Long roleId);

    Optional<RoleMenuAccess> findByRoleIdAndMenuId(Long roleId, Long menuId);

    void deleteByRoleId(Long roleId);
}
