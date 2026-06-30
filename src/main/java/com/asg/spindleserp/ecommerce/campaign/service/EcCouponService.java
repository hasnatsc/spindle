// Path: com/asg/spindleserp/ecommerce/service/EcCouponService.java
package com.asg.spindleserp.ecommerce.campaign.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.campaign.dto.EcCouponDTO;
import com.asg.spindleserp.ecommerce.campaign.entity.EcCoupon;

import java.util.List;
public interface EcCouponService {
    EcCouponDTO create(EcCouponDTO dto);
    EcCouponDTO update(Long id, EcCouponDTO dto);
    EcCouponDTO findById(Long id);
    List<EcCouponDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    EcCouponDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcCouponDTO toDTO(EcCoupon entity);
}
