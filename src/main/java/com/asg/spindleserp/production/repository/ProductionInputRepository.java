package com.asg.spindleserp.production.repository;

import com.asg.spindleserp.production.entity.ProductionInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionInputRepository extends JpaRepository<ProductionInput, Long> {
    List<ProductionInput> findByProductionIdOrderByLineNumber(Long productionId);

    List<ProductionInput> findByRawItemId(Long rawItemId);

    List<ProductionInput> findByLotId(Long lotId);

    // Sum material cost for a production — after all inputs recorded
    @Query("SELECT COALESCE(SUM(pi.totalCost), 0) FROM ProductionInput pi " +
            "WHERE pi.production.id = :productionId")
    java.math.BigDecimal sumTotalCostByProduction(@Param("productionId") Long productionId);
}
