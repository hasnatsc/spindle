package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.PaymentAccount;
import com.asg.spindleserp.common.enums.PaymentMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ★ Assumes BaseEntity exposes an {@code organizationId} property. If your
 *   BaseEntity names it differently, rename it in the derived queries below.
 */
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {

    Optional<PaymentAccount> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<PaymentAccount> findByOrganizationIdAndPaymentAccountCode(Long organizationId, String code);

    boolean existsByOrganizationIdAndPaymentAccountCode(Long organizationId, String code);

    boolean existsByOrganizationIdAndPaymentAccountCodeAndIdNot(Long organizationId, String code, Long id);

    List<PaymentAccount> findByOrganizationIdAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(
            Long organizationId);

    List<PaymentAccount> findByOrganizationIdAndPaymentModeAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(
            Long organizationId, PaymentMode paymentMode);

    List<PaymentAccount> findByOrganizationIdAndAccountCategoryAndIsActiveTrueOrderBySortOrderAscPaymentAccountNameAsc(
            Long organizationId, PaymentMode.AccountCategory accountCategory);

    Optional<PaymentAccount> findFirstByOrganizationIdAndPaymentModeAndIsDefaultTrueAndIsActiveTrue(
            Long organizationId, PaymentMode paymentMode);

    /** Enforces one-default-per-mode; call before flipping isDefault on. */
    @Modifying
    @Query("update PaymentAccount p set p.isDefault = false " +
           "where p.organization.id = :orgId and p.paymentMode = :mode and p.id <> :keepId")
    int clearDefaultForMode(@Param("orgId") Long orgId,
                            @Param("mode") PaymentMode mode,
                            @Param("keepId") Long keepId);

    /** Referential guard before delete — blocks removal once vouchers point at it. */
    @Query("select count(j) from JournalEntryMaster j where j.paymentAccountId = :id")
    long countVouchersUsing(@Param("id") Long id);
}
