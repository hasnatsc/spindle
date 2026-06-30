// Path: com/asg/spindleserp/ecommerce/service/EcTaxClassService.java
package com.asg.spindleserp.ecommerce.settings.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.settings.dto.EcTaxClassDTO;
import com.asg.spindleserp.ecommerce.settings.entity.EcTaxClass;

import java.util.List;
public interface EcTaxClassService {
    EcTaxClassDTO create(EcTaxClassDTO dto);
    EcTaxClassDTO update(Long id, EcTaxClassDTO dto);
    EcTaxClassDTO findById(Long id);
    List<EcTaxClassDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    EcTaxClassDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcTaxClassDTO toDTO(EcTaxClass entity);
}
