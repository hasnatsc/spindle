// Path: com/asg/spindleserp/ecommerce/repository/EcRefundRepository.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.repository;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface EcRefundRepository extends JpaRepository<EcRefund, Long> {
    List<EcRefund> findByEcReturnIdOrderByIdDesc(Long returnId);
    boolean existsByEcReturnIdAndRefundNo(Long returnId, String refundNo);
}
