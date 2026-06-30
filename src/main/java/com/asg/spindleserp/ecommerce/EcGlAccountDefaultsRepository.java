// Path: com/asg/spindleserp/ecommerce/repository/EcGlAccountDefaultsRepository.java
package com.asg.spindleserp.ecommerce;
import com.asg.spindleserp.ecommerce.EcGlAccountDefaults;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface EcGlAccountDefaultsRepository extends JpaRepository<EcGlAccountDefaults, Long> {
    Optional<EcGlAccountDefaults> findByOrganizationId(Long orgId);
}
