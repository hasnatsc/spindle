// Path: com/asg/spindleserp/ecommerce/repository/EcTaxClassRepository.java
package com.asg.spindleserp.ecommerce.settings.repository;

import com.asg.spindleserp.ecommerce.settings.entity.EcTaxClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EcTaxClassRepository extends JpaRepository<EcTaxClass, Long> {
    boolean existsByOrganizationIdAndClassCode(Long orgId, String code);
    boolean existsByOrganizationIdAndClassCodeAndIdNot(Long orgId, String code, Long id);
    List<EcTaxClass> findByOrganizationIdAndActiveTrue(Long orgId);
    List<EcTaxClass> findByOrganizationIdOrderByIdDesc(Long orgId);
}
