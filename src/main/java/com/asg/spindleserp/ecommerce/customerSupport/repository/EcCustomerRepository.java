// Path: com/asg/spindleserp/ecommerce/repository/EcCustomerRepository.java
package com.asg.spindleserp.ecommerce.customerSupport.repository;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcCustomerRepository extends JpaRepository<EcCustomer, Long> {
    boolean existsByOrganizationIdAndCustomerCode(Long orgId, String code);
    boolean existsByOrganizationIdAndCustomerCodeAndIdNot(Long orgId, String code, Long id);
    boolean existsByOrganizationIdAndPhone(Long orgId, String phone);
    boolean existsByOrganizationIdAndPhoneAndIdNot(Long orgId, String phone, Long id);
    List<EcCustomer> findByOrganizationIdAndDeletedFalse(Long orgId);
    List<EcCustomer> findByOrganizationIdAndActiveTrueAndDeletedFalse(Long orgId);
    Optional<EcCustomer> findByOrganizationIdAndPhone(Long orgId, String phone);
}
