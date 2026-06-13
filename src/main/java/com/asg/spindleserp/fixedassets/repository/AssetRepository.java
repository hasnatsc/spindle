package com.asg.spindleserp.fixedassets.repository;

import com.asg.spindleserp.fixedassets.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
    Optional<Asset> findByOrganizationIdAndAssetCode(Long orgId, String code);

    List<Asset> findByOrganizationIdAndStatus(Long orgId, Asset.AssetStatus status);

    long countByOrganizationIdAndStatus(Long orgId, Asset.AssetStatus status);

    boolean existsByOrganizationIdAndAssetCode(Long orgId, String code);
}
