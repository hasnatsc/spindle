package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    List<Role> findByActiveTrue();

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    /** Roles for user-assignment dropdowns — permissions eagerly loaded */
    @Query("""
        SELECT DISTINCT r FROM Role r
        LEFT JOIN FETCH r.permissions
        WHERE r.active = true
        ORDER BY r.name ASC
        """)
    List<Role> findAllActiveWithPermissions();

    /** All roles ordered — for DataTable and dropdowns */
    List<Role> findAllByOrderByNameAsc();
}
