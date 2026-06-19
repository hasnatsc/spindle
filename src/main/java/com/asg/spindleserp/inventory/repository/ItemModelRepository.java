package com.asg.spindleserp.inventory.repository;

import com.asg.spindleserp.inventory.entity.ItemModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemModelRepository
        extends JpaRepository<ItemModel, Long>,
                JpaSpecificationExecutor<ItemModel> {

    List<ItemModel> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<ItemModel> findByBrandIdAndIsActiveTrue(Long brandId);

    boolean existsByOrganizationIdAndBrandIdAndModelCode(Long orgId, Long brandId, String code);

    boolean existsByOrganizationIdAndBrandIdAndModelCodeAndIdNot(Long orgId, Long brandId, String code, Long id);
}
