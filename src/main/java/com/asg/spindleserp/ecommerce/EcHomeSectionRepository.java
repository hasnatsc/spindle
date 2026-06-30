// Path: com/asg/spindleserp/ecommerce/repository/EcHomeSectionRepository.java
package com.asg.spindleserp.ecommerce;
import com.asg.spindleserp.ecommerce.cms.EcHomeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface EcHomeSectionRepository extends JpaRepository<EcHomeSection, Long> {
    boolean existsByOrganizationIdAndSectionCode(Long orgId, String code);
    boolean existsByOrganizationIdAndSectionCodeAndIdNot(Long orgId, String code, Long id);
    List<EcHomeSection> findByOrganizationIdOrderByDisplayOrder(Long orgId);
    List<EcHomeSection> findByOrganizationIdAndActiveTrueOrderByDisplayOrder(Long orgId);
}
