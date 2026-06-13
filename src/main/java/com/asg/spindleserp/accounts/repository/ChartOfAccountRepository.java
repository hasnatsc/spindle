package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long>, JpaSpecificationExecutor<ChartOfAccount> {
    Optional<ChartOfAccount> findByAccountCode(String code);

    List<ChartOfAccount> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ChartOfAccount> findByOrganizationIdAndAccountType(Long orgId, ChartOfAccount.AccountType type);

    List<ChartOfAccount> findByParentAccountIdIsNullAndOrganizationId(Long orgId);

    boolean existsByAccountCode(String code);

    boolean existsByOrganizationIdAndAccountCode(Long orgId, String code);
}
