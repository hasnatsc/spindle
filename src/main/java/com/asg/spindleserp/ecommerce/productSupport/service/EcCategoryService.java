// ── 1. EcCategoryService ─────────────────────────────────────────────────────
// Path: com/asg/spindleserp/ecommerce/service/EcCategoryService.java
package com.asg.spindleserp.ecommerce.productSupport.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.productSupport.dto.EcCategoryDTO;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
public interface EcCategoryService {
    /** Upload the category image or banner file. type = "image" | "banner". */
    EcCategoryDTO uploadImage(Long id, String type, MultipartFile file);
    /** Remove the category image or banner (clears the field, deletes local file). */
    EcCategoryDTO removeImage(Long id, String type);
    EcCategoryDTO create(EcCategoryDTO dto);
    EcCategoryDTO update(Long id, EcCategoryDTO dto);
    EcCategoryDTO findById(Long id);
    List<EcCategoryDTO> findActiveByOrg(Long orgId);
    List<EcCategoryDTO> findAllByOrg(Long orgId);
    void delete(Long id);
    EcCategoryDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcCategoryDTO toDTO(EcCategory entity);
}
