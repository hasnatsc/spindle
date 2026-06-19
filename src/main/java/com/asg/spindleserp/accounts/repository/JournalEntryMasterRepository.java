package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.common.enums.VoucherType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JournalEntryMasterRepository
 *
 * Includes:
 *  - Standard finders (by voucher no, org, posted flag)
 *  - Atomic allocation updaters (addAllocation / subtractAllocation)
 *  - AP/AR open-voucher query for the allocation picker
 *  - Aging support helpers
 */
@Repository
public interface JournalEntryMasterRepository
        extends JpaRepository<JournalEntryMaster, Long>,
                JpaSpecificationExecutor<JournalEntryMaster> {

    Optional<JournalEntryMaster> findByVoucherNo(String voucherNo);

    List<JournalEntryMaster> findByOrganizationIdAndIsPosted(Long orgId, boolean posted);

    long countByOrganizationIdAndIsPosted(Long orgId, boolean posted);

    boolean existsByVoucherNo(String voucherNo);

    List<JournalEntryMaster> findByOrganizationIdAndVoucherStatus(Long orgId, String status);

    List<JournalEntryMaster> findByOrganizationIdAndVoucherTypeAndVoucherStatus(
            Long orgId, VoucherType voucherType, String status);

    @Query("""
            SELECT j FROM JournalEntryMaster j
            WHERE j.organization.id = :orgId
              AND j.partyId         = :partyId
              AND j.voucherStatus   = 'POSTED'
              AND j.totalAmount     > j.allocatedAmount
            ORDER BY j.dueDate ASC NULLS LAST, j.voucherDate ASC
            """)
    List<JournalEntryMaster> findOpenForParty(@Param("orgId")   Long orgId,
                                               @Param("partyId") Long partyId);

    @Query("""
            SELECT j FROM JournalEntryMaster j
            WHERE j.organization.id = :orgId
              AND j.partyId         = :partyId
              AND j.partyType       = :partyType
              AND j.voucherStatus   = 'POSTED'
              AND j.totalAmount     > j.allocatedAmount
            ORDER BY j.dueDate ASC NULLS LAST, j.voucherDate ASC
            """)
    List<JournalEntryMaster> findOpenForPartyAndType(@Param("orgId")     Long orgId,
                                                      @Param("partyId")   Long partyId,
                                                      @Param("partyType") String partyType);

    @Modifying
    @Query("""
            UPDATE JournalEntryMaster j
               SET j.allocatedAmount = j.allocatedAmount + :amount
             WHERE j.id = :id
            """)
    void addAllocation(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Returns the number of rows updated (0 if allocatedAmount < amount — safety guard).
     */
    @Modifying
    @Query("""
            UPDATE JournalEntryMaster j
               SET j.allocatedAmount = j.allocatedAmount - :amount
             WHERE j.id = :id
               AND j.allocatedAmount >= :amount
            """)
    int subtractAllocation(@Param("id") Long id, @Param("amount") BigDecimal amount);

    long countByOrganizationIdAndVoucherTypeAndVoucherStatus(
            Long orgId, VoucherType voucherType, String status);

    @Query("""
            SELECT COALESCE(SUM(j.totalAmount - j.allocatedAmount), 0)
            FROM JournalEntryMaster j
            WHERE j.organization.id = :orgId
              AND j.partyType       = :partyType
              AND j.voucherStatus   = 'POSTED'
              AND j.totalAmount     > j.allocatedAmount
            """)
    BigDecimal sumOutstandingByPartyType(@Param("orgId")     Long orgId,
                                          @Param("partyType") String partyType);
}
