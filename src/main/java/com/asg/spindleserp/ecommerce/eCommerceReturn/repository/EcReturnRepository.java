// Path: com/asg/spindleserp/ecommerce/repository/EcReturnRepository.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.repository;

import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcReturnRepository extends JpaRepository<EcReturn, Long> {
    boolean existsByOrganizationIdAndReturnNo(Long orgId, String returnNo);
    Optional<EcReturn> findByOrganizationIdAndReturnNo(Long orgId, String returnNo);
    List<EcReturn> findByOrganizationIdOrderByIdDesc(Long orgId);
    List<EcReturn> findByOrderIdOrderByIdDesc(Long orderId);
}
