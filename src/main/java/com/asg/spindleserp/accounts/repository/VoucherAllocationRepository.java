package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.VoucherAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VoucherAllocationRepository extends JpaRepository<VoucherAllocation, Long> {

    List<VoucherAllocation> findBySourceVoucherId(Long sourceVoucherId);

    List<VoucherAllocation> findByPayingVoucherId(Long payingVoucherId);

    @Query("SELECT COALESCE(SUM(a.allocatedAmount + a.discountAmount + a.writeOffAmount), 0) " +
           "FROM VoucherAllocation a WHERE a.sourceVoucher.id = :id")
    BigDecimal sumAllocatedForSource(@Param("id") Long sourceVoucherId);

    @Modifying
    @Query("DELETE FROM VoucherAllocation a WHERE a.payingVoucher.id = :payingId")
    void deleteByPayingVoucherId(@Param("payingId") Long payingVoucherId);

    boolean existsBySourceVoucherIdAndPayingVoucherId(Long sourceId, Long payingId);
}
