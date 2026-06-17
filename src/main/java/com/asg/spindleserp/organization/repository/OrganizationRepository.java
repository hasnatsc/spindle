package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository
        extends JpaRepository<Organization, Long>,
                JpaSpecificationExecutor<Organization> {

    Optional<Organization> findByCode(String code);

    List<Organization> findByIsActiveTrue();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
