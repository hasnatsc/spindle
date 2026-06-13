package com.asg.spindleserp.production.repository;

import com.asg.spindleserp.production.entity.Production;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRepository extends JpaRepository<Production, Long>, JpaSpecificationExecutor<Production> {
    Optional<Production> findByOrganizationIdAndProductionNo(Long orgId, String no);

    List<Production> findByOrganizationIdAndStatus(Long orgId, Production.ProductionStatus status);

    List<Production> findByOrganizationIdAndFinishedItemId(Long orgId, Long itemId);

    List<Production> findByCostCenterId(Long costCenterId);

    boolean existsByOrganizationIdAndProductionNo(Long orgId, String no);

    // For labor cost proportioning
    @Query("SELECT COALESCE(SUM(p.producedQuantity), 0) FROM Production p " +
            "WHERE p.costCenter.id = :costCenterId " +
            "AND FUNCTION('TO_CHAR', p.productionDate, 'YYYY-MM') = :month " +
            "AND p.status = 'COMPLETED'")
    BigDecimal sumProducedQtyByCostCenterAndMonth(
            @Param("costCenterId") Long costCenterId,
            @Param("month") String month);
}
