// Path: com/asg/spindleserp/ecommerce/repository/EcPaymentRepository.java
package com.asg.spindleserp.ecommerce.payment.repository;
import com.asg.spindleserp.ecommerce.payment.entity.EcPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EcPaymentRepository extends JpaRepository<EcPayment, Long> {
    Optional<EcPayment> findByOrganizationIdAndPaymentNo(Long orgId, String paymentNo);
    boolean existsByOrganizationIdAndPaymentNo(Long orgId, String paymentNo);
    List<EcPayment> findByOrderIdOrderByIdDesc(Long orderId);
    List<EcPayment> findByOrganizationIdOrderByIdDesc(Long orgId);
}
