package com.asg.spindleserp.inventory.repository;

import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCategoryRepository
        extends JpaRepository<ItemCategory, Long>,
                JpaSpecificationExecutor<ItemCategory> {

    List<ItemCategory> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ItemCategory> findByOrganizationIdAndItemTypeAndIsActiveTrue(Long orgId, ItemType type);

    List<ItemCategory> findByParentCategoryIdIsNullAndOrganizationId(Long orgId);

    Optional<ItemCategory> findByOrganizationIdAndCategoryCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCategoryCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCategoryCodeAndIdNot(Long orgId, String code, Long id);

    boolean existsByParentCategoryId(Long parentId);
}
