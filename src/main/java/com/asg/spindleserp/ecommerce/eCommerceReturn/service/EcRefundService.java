// Path: com/asg/spindleserp/ecommerce/service/EcRefundService.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.eCommerceReturn.dto.EcRefundDTO;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcRefund;

public interface EcRefundService {
    EcRefundDTO create(EcRefundDTO dto);
    EcRefundDTO findById(Long id);
    void delete(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcRefundDTO toDTO(EcRefund entity);
}
