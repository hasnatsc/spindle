package com.asg.spindleserp.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    List<ItemCategory> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ItemCategory> findByOrganizationIdAndItemTypeAndIsActiveTrue(Long orgId, ItemType type);

    List<ItemCategory> findByParentCategoryIdIsNullAndOrganizationId(Long orgId);

    Optional<ItemCategory> findByOrganizationIdAndCategoryCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCategoryCode(Long orgId, String code);
}
