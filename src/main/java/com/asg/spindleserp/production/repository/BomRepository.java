package com.asg.spindleserp.production.repository;

import com.asg.spindleserp.production.entity.Bom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long>, JpaSpecificationExecutor<Bom> {
    Optional<Bom> findByOrganizationIdAndBomCode(Long orgId, String code);

    List<Bom> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<Bom> findByFinishedItemIdAndIsActiveTrue(Long itemId);

    Optional<Bom> findByFinishedItemIdAndIsDefaultTrue(Long itemId);

    boolean existsByOrganizationIdAndBomCode(Long orgId, String code);
}
