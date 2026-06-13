package com.asg.spindleserp.security.repository;

import com.asg.spindleserp.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    List<Role> findByActiveTrue();

    boolean existsByName(String name);
}
