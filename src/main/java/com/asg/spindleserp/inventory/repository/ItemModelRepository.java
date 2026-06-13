package com.asg.spindleserp.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemModelRepository extends JpaRepository<ItemModel, Long> {
    List<ItemModel> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ItemModel> findByBrandIdAndIsActiveTrue(Long brandId);

    boolean existsByOrganizationIdAndBrandIdAndModelCode(Long orgId, Long brandId, String code);
}
