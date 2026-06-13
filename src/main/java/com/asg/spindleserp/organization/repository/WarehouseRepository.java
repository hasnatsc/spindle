package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByBusinessUnitIdAndIsActiveTrue(Long buId);

    List<Warehouse> findByBusinessUnitOrganizationIdAndIsActiveTrue(Long orgId);

    Optional<Warehouse> findByWarehouseCode(String code);

    boolean existsByWarehouseCode(String code);
}
