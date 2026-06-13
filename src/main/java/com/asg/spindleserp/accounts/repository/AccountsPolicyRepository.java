package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.AccountsPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountsPolicyRepository extends JpaRepository<AccountsPolicy, Long> {
    Optional<AccountsPolicy> findByOrganizationIdAndPolicyCode(Long orgId, String code);

    Optional<AccountsPolicy> findByOrganizationIdAndModuleTypeAndIsDefaultTrue(Long orgId, String module);
}
