// Path: com/asg/spindleserp/ecommerce/service/EcProductCatalogService.java
package com.asg.spindleserp.ecommerce.productSupport.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.dto.EcProductCatalogDTO;

import java.util.List;

public interface EcProductCatalogService {

    EcProductCatalogDTO create(EcProductCatalogDTO dto);
    EcProductCatalogDTO update(Long id, EcProductCatalogDTO dto);
    EcProductCatalogDTO findById(Long id);
    List<EcProductCatalogDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    EcProductCatalogDTO toggleStatus(Long id);
    EcProductCatalogDTO togglePublished(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcProductCatalogDTO toDTO(EcProductCatalog entity);
}
