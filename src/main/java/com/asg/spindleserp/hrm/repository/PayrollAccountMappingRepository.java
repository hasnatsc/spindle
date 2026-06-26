package com.asg.spindleserp.hrm.repository;

import com.asg.spindleserp.hrm.entity.PayrollAccountMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollAccountMappingRepository extends JpaRepository<PayrollAccountMapping, Long> {
    Optional<PayrollAccountMapping> findByOrganizationId(Long organizationId);
}