package com.asg.spindleserp.setup.repository;

import com.asg.spindleserp.setup.entity.HsCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HsCodeRepository
        extends JpaRepository<HsCode, Long>,
                JpaSpecificationExecutor<HsCode> {

    Optional<HsCode> findByOrganizationIdAndHsCode(Long orgId, String hsCode);

    List<HsCode> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<HsCode> findByOrganizationIdAndIsActiveTrueAndHsType(Long orgId, HsCode.HsType hsType);

    boolean existsByOrganizationIdAndHsCode(Long orgId, String hsCode);

    boolean existsByOrganizationIdAndHsCodeAndIdNot(Long orgId, String hsCode, Long id);
}
