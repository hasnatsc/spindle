package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.Role;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    List<Role> findByActiveTrue();

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    /** For role assignment dropdowns — load with permissions to avoid N+1 */
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.active = true")
    List<Role> findAllActiveWithPermissions();
}
