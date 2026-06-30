// Path: com/asg/spindleserp/ecommerce/repository/EcCouponRepository.java
package com.asg.spindleserp.ecommerce.campaign.repository;

import com.asg.spindleserp.ecommerce.campaign.entity.EcCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcCouponRepository extends JpaRepository<EcCoupon, Long> {
    boolean existsByOrganizationIdAndCouponCode(Long orgId, String code);
    boolean existsByOrganizationIdAndCouponCodeAndIdNot(Long orgId, String code, Long id);
    List<EcCoupon> findByOrganizationIdAndActiveTrue(Long orgId);
    Optional<EcCoupon> findByOrganizationIdAndCouponCode(Long orgId, String code);
}
