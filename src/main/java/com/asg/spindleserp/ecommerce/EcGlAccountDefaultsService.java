// Path: com/asg/spindleserp/ecommerce/service/EcGlAccountDefaultsService.java
package com.asg.spindleserp.ecommerce;
import com.asg.spindleserp.ecommerce.EcGlAccountDefaults;
public interface EcGlAccountDefaultsService {
    EcGlAccountDefaultsDTO findOrCreateByOrg(Long orgId);
    EcGlAccountDefaultsDTO save(EcGlAccountDefaultsDTO dto);
    EcGlAccountDefaultsDTO toDTO(EcGlAccountDefaults entity);
}
