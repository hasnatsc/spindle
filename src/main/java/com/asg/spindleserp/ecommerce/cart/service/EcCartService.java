// Path: com/asg/spindleserp/ecommerce/service/EcCartService.java
package com.asg.spindleserp.ecommerce.cart.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.cart.dto.EcCartDTO;
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;

public interface EcCartService {
    EcCartDTO findById(Long id);
    EcCartDTO markAbandoned(Long id);
    void delete(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcCartDTO toDTO(EcCart entity);
}
