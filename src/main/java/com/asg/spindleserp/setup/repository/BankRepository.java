package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository
        extends JpaRepository<Bank, Long>,
                JpaSpecificationExecutor<Bank> {

    Optional<Bank> findByOrganizationIdAndBankCode(Long orgId, String bankCode);

    List<Bank> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<Bank> findByOrganizationIdAndIsActiveTrueAndSupportsLcTrue(Long orgId);

    boolean existsByOrganizationIdAndBankCode(Long orgId, String bankCode);

    boolean existsByOrganizationIdAndBankCodeAndIdNot(Long orgId, String bankCode, Long id);
}
