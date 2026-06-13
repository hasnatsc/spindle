package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {
    List<AccountingPeriod> findByOrganizationIdAndIsActiveTrue(Long orgId);

    Optional<AccountingPeriod> findByOrganizationIdAndIsActiveTrueAndIsClosedFalse(Long orgId);

    List<AccountingPeriod> findByOrganizationIdAndFiscalYear(Long orgId, int year);
}
