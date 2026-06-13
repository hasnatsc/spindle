package com.asg.spindleserp.fixedassets.repository;

import com.asg.spindleserp.fixedassets.entity.AssetDisposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetDisposalRepository extends JpaRepository<AssetDisposal, Long> {
    Optional<AssetDisposal> findByAssetId(Long assetId);
}
