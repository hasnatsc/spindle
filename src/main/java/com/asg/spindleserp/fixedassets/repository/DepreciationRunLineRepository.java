package com.asg.spindleserp.fixedassets.repository;

import com.asg.spindleserp.fixedassets.entity.DepreciationRunLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepreciationRunLineRepository extends JpaRepository<DepreciationRunLine, Long> {
    List<DepreciationRunLine> findByDepreciationRunId(Long runId);

    List<DepreciationRunLine> findByAssetId(Long assetId);
}
