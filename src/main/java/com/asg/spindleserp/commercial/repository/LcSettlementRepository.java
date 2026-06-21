package com.asg.spindleserp.commercial.repository;

import com.asg.spindleserp.commercial.entity.LcSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LcSettlementRepository extends JpaRepository<LcSettlement, Long> {
    List<LcSettlement> findByLcId(Long lcId);
    List<LcSettlement> findByStatus(LcSettlement.SettlementStatus status);
}
