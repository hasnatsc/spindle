package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCode(String code);

    List<Organization> findByIsActiveTrue();

    boolean existsByCode(String code);
}
