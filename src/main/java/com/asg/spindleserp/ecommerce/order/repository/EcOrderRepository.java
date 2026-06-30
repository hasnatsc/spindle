// Path: com/asg/spindleserp/ecommerce/repository/EcOrderRepository.java
package com.asg.spindleserp.ecommerce.order.repository;

import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcOrderRepository extends JpaRepository<EcOrder, Long> {
    Optional<EcOrder> findByOrganizationIdAndOrderNo(Long orgId, String orderNo);
    boolean existsByOrganizationIdAndOrderNo(Long orgId, String orderNo);
    List<EcOrder> findByOrganizationIdAndActiveTrueOrderByIdDesc(Long orgId);
    List<EcOrder> findByCustomerIdOrderByIdDesc(Long customerId);
}
