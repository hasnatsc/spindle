// Path: com/asg/spindleserp/ecommerce/service/EcPaymentService.java
package com.asg.spindleserp.ecommerce.payment.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.payment.dto.EcPaymentDTO;
import com.asg.spindleserp.ecommerce.payment.entity.EcPayment;

public interface EcPaymentService {
    EcPaymentDTO findById(Long id);
    EcPaymentDTO updateStatus(Long id, String newStatus, String remarks);
    void delete(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcPaymentDTO toDTO(EcPayment entity);
}
