package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ChartOfAccountRepository
 *
 * ADD these three methods to your existing repository interface.
 * They are used by PurchaseServiceImpl.resolvePurchaseAccount() to find
 * the DR (Purchases/Expense) account for the auto-generated PURCHASE_VOUCHER.
 *
 * Priority order in resolvePurchaseAccount():
 *   1. findByOrganizationIdAndAccountCodeIgnoreCase(orgId, "PURCHASE-ACCOUNT")
 *   2. findFirstByOrganizationIdAndAccountTypeAndAccountCodeStartingWithIgnoreCaseAndIsActiveTrue
 *   3. findFirstByOrganizationIdAndAccountTypeAndIsActiveTrue (any EXPENSE)
 *
 * HOW TO CONFIGURE:
 *   Go to Accounts → Chart of Accounts → create (or rename) one account:
 *     Account Code : PURCHASE-ACCOUNT
 *     Account Type : EXPENSE
 *     Account Name : Purchases / Cost of Goods Purchased
 *   This becomes the DR side of every auto-generated Purchase Invoice voucher.
 *   If not found, any active EXPENSE account starting with 'PURCH' is used as fallback.
 */
@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    Optional<ChartOfAccount> findByAccountCode(String code);

    List<ChartOfAccount> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ChartOfAccount> findByOrganizationIdAndAccountType(Long orgId, ChartOfAccount.AccountType type);

    List<ChartOfAccount> findByParentAccountIdIsNullAndOrganizationId(Long orgId);

    boolean existsByAccountCode(String code);

    boolean existsByOrganizationIdAndAccountCode(Long orgId, String code);

    /** Priority 1: exact code match (recommended setup) */
    Optional<ChartOfAccount> findByOrganizationIdAndAccountCodeIgnoreCase(
            Long organizationId, String accountCode);

    /** Priority 2: PURCH* EXPENSE account for this org */
    Optional<ChartOfAccount> findFirstByOrganizationIdAndAccountTypeAndAccountCodeStartingWithIgnoreCaseAndIsActiveTrue(
            Long organizationId,
            ChartOfAccount.AccountType accountType,
            String accountCodePrefix);

    /** Priority 3: any active EXPENSE account for this org */
    Optional<ChartOfAccount> findFirstByOrganizationIdAndAccountTypeAndIsActiveTrue(
            Long organizationId,
            ChartOfAccount.AccountType accountType);
}
