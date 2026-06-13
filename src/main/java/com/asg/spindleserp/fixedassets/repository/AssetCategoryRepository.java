package com.asg.spindleserp.fixedassets.repository;

import com.asg.spindleserp.fixedassets.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    List<AssetCategory> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<AssetCategory> findByParentIdIsNullAndOrganizationId(Long orgId);

    Optional<AssetCategory> findByOrganizationIdAndCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCode(Long orgId, String code);
}
