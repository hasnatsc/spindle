// Path: com/asg/spindleserp/ecommerce/repository/EcShippingMethodRepository.java
package com.asg.spindleserp.ecommerce.shipping.repository;
import com.asg.spindleserp.ecommerce.shipping.entity.EcShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface EcShippingMethodRepository extends JpaRepository<EcShippingMethod, Long> {
    boolean existsByOrganizationIdAndMethodCode(Long orgId, String code);
    boolean existsByOrganizationIdAndMethodCodeAndIdNot(Long orgId, String code, Long id);
    List<EcShippingMethod> findByOrganizationIdAndActiveTrue(Long orgId);
}
