// Path: com/asg/spindleserp/ecommerce/service/EcOrderService.java
package com.asg.spindleserp.ecommerce.order.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.order.dto.EcOrderDTO;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;

import java.util.List;
public interface EcOrderService {
    EcOrderDTO findById(Long id);
    EcOrderDTO updateStatus(Long id, String newStatus, String adminNote);
    EcOrderDTO updateAdminNote(Long id, String note);
    void delete(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcOrderDTO toDTO(EcOrder entity);
}
