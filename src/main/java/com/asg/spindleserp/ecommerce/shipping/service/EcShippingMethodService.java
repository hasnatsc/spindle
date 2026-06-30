// Path: com/asg/spindleserp/ecommerce/service/EcShippingMethodService.java
package com.asg.spindleserp.ecommerce.shipping.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.shipping.dto.EcShippingMethodDTO;
import com.asg.spindleserp.ecommerce.shipping.entity.EcShippingMethod;

import java.util.List;
public interface EcShippingMethodService {
    EcShippingMethodDTO create(EcShippingMethodDTO dto);
    EcShippingMethodDTO update(Long id, EcShippingMethodDTO dto);
    EcShippingMethodDTO findById(Long id);
    List<EcShippingMethodDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    EcShippingMethodDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcShippingMethodDTO toDTO(EcShippingMethod entity);
}
