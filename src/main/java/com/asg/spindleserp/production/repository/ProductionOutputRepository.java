package com.asg.spindleserp.production.repository;

import com.asg.spindleserp.production.entity.ProductionOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionOutputRepository extends JpaRepository<ProductionOutput, Long> {
    List<ProductionOutput> findByProductionIdOrderByLineNumber(Long productionId);

    List<ProductionOutput> findByFinishedItemId(Long itemId);

    List<ProductionOutput> findByLotId(Long lotId);
}
