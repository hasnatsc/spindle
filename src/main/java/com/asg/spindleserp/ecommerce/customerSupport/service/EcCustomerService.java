// Path: com/asg/spindleserp/ecommerce/service/EcCustomerService.java
package com.asg.spindleserp.ecommerce.customerSupport.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.customerSupport.dto.EcCustomerDTO;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;

import java.util.List;
public interface EcCustomerService {
    EcCustomerDTO create(EcCustomerDTO dto);
    EcCustomerDTO update(Long id, EcCustomerDTO dto);
    EcCustomerDTO findById(Long id);
    List<EcCustomerDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    EcCustomerDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcCustomerDTO toDTO(EcCustomer entity);
}
