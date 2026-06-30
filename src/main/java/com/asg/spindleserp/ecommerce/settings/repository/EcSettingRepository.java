// Path: com/asg/spindleserp/ecommerce/repository/EcSettingRepository.java
package com.asg.spindleserp.ecommerce.settings.repository;
import com.asg.spindleserp.ecommerce.settings.entity.EcSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EcSettingRepository extends JpaRepository<EcSetting, Long> {
    List<EcSetting> findByOrganizationIdOrderBySettingGroupAscSettingKeyAsc(Long orgId);
    List<EcSetting> findByOrganizationIdAndSettingGroupOrderBySettingKeyAsc(Long orgId, String group);
    Optional<EcSetting> findByOrganizationIdAndSettingGroupAndSettingKey(Long orgId, String group, String key);
    boolean existsByOrganizationIdAndSettingGroupAndSettingKey(Long orgId, String group, String key);
    boolean existsByOrganizationIdAndSettingGroupAndSettingKeyAndIdNot(Long orgId, String group, String key, Long id);
}
