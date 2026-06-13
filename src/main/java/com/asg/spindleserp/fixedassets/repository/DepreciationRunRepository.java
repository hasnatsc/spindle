package com.asg.spindleserp.fixedassets.repository;

import com.asg.spindleserp.fixedassets.entity.DepreciationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepreciationRunRepository extends JpaRepository<DepreciationRun, Long> {
    List<DepreciationRun> findByOrganizationIdAndStatus(Long orgId, DepreciationRun.RunStatus status);

    Optional<DepreciationRun> findTopByOrganizationIdAndStatusOrderByRunDateDesc(
            Long orgId, DepreciationRun.RunStatus status);
}
