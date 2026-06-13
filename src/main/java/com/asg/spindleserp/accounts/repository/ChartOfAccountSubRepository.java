package com.asg.spindleserp.accounts.repository;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountSubRepository extends JpaRepository<ChartOfAccountSub, Long>, JpaSpecificationExecutor<ChartOfAccountSub> {
    Optional<ChartOfAccountSub> findBySubAccountCode(String code);

    List<ChartOfAccountSub> findByOrganizationIdAndSubAccountTypeAndIsActiveTrue(Long orgId, String type);

    List<ChartOfAccountSub> findByOrganizationIdAndIsActiveTrue(Long orgId);

    @Query("SELECT s FROM ChartOfAccountSub s WHERE s.organizationId = :orgId " +
//            "AND s.subAccountType = :type AND s.isActive = true " +
            "AND (LOWER(s.subAccountCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
            "OR   LOWER(s.subAccountName) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<ChartOfAccountSub> search(@Param("orgId") Long orgId,
                                   @Param("type") String type,
                                   @Param("q") String q);
}
