package com.asg.spindleserp.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemBrandRepository extends JpaRepository<ItemBrand, Long> {
    List<ItemBrand> findByOrganizationIdAndIsActiveTrue(Long orgId);

    Optional<ItemBrand> findByOrganizationIdAndBrandCode(Long orgId, String code);

    boolean existsByOrganizationIdAndBrandCode(Long orgId, String code);
}
