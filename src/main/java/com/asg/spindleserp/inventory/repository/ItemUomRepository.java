package com.asg.spindleserp.inventory.repository;

import com.asg.spindleserp.inventory.entity.ItemUom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemUomRepository extends JpaRepository<ItemUom, Long> {
    List<ItemUom> findByOrganizationIdAndActive(Long orgId, boolean active);

    Optional<ItemUom> findByOrganizationIdAndCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCode(Long orgId, String code);
}
