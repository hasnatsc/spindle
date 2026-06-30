// Path: com/asg/spindleserp/ecommerce/service/EcHomeSectionService.java
package com.asg.spindleserp.ecommerce;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.cms.EcHomeSection;
import java.util.List;
public interface EcHomeSectionService {
    EcHomeSectionDTO create(EcHomeSectionDTO dto);
    EcHomeSectionDTO update(Long id, EcHomeSectionDTO dto);
    EcHomeSectionDTO findById(Long id);
    List<EcHomeSectionDTO> findAllByOrg(Long orgId);
    void delete(Long id);
    EcHomeSectionDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcHomeSectionDTO toDTO(EcHomeSection entity);
}
