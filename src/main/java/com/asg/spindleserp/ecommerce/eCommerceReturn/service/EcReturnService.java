// Path: com/asg/spindleserp/ecommerce/service/EcReturnService.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.eCommerceReturn.dto.EcReturnDTO;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcReturn;

import java.util.List;
public interface EcReturnService {
    EcReturnDTO create(EcReturnDTO dto);
    EcReturnDTO findById(Long id);
    EcReturnDTO updateStatus(Long id, String status, String remarks);
    void delete(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcReturnDTO toDTO(EcReturn entity);
}
