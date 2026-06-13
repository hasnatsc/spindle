package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.AccountsMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsMappingRepository extends JpaRepository<AccountsMapping, Long> {
    Optional<AccountsMapping> findByOrganizationIdAndMappingCode(Long orgId, String code);

    List<AccountsMapping> findByOrganizationIdAndModuleTypeAndIsActiveTrue(Long orgId, String module);

    Optional<AccountsMapping> findByOrganizationIdAndTransactionTypeAndIsDefaultTrue(Long orgId, String txType);
}
