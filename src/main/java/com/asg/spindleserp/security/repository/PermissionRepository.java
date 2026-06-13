package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.Permission;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /** Used by DynamicAuthorizationManager — loaded once, cached in memory. */
    @Query("SELECT p FROM Permission p WHERE p.active = true ORDER BY p.module, p.name")
    List<Permission> findAllActive();

    Optional<Permission> findByName(String name);

    List<Permission> findByModuleAndActiveTrue(String module);
    List<Permission> findByActiveTrue();

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
