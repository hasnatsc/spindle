// Path: com/asg/spindleserp/ecommerce/repository/EcCartRepository.java
package com.asg.spindleserp.ecommerce.cart.repository;
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EcCartRepository extends JpaRepository<EcCart, Long> {
    List<EcCart> findByOrganizationIdOrderByIdDesc(Long orgId);
    List<EcCart> findByOrganizationIdAndCartStatus(Long orgId, EcCart.CartStatus status);
    Optional<EcCart> findByCustomerIdAndCartStatus(Long customerId, EcCart.CartStatus status);
    Optional<EcCart> findBySessionIdAndCartStatus(String sessionId, EcCart.CartStatus status);
}
