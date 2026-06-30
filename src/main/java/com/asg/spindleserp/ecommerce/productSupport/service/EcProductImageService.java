// Path: com/asg/spindleserp/ecommerce/productSupport/service/EcProductImageService.java
package com.asg.spindleserp.ecommerce.productSupport.service;

import com.asg.spindleserp.ecommerce.productSupport.dto.EcProductImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EcProductImageService {

    /** Upload a new image file for a product. Returns the saved row. */
    EcProductImageDTO upload(Long productId, MultipartFile file, String altText, Boolean isPrimary);

    /** Add an image from a manually-pasted external URL (CDN workflow, kept as a fallback). */
    EcProductImageDTO addByUrl(Long productId, EcProductImageDTO dto);

    /** Update metadata (alt text, display order, primary flag) for one image — no file re-upload. */
    EcProductImageDTO update(Long productId, Long imageId, EcProductImageDTO dto);

    /** Replace the file behind an existing image row (re-upload). */
    EcProductImageDTO replaceFile(Long productId, Long imageId, MultipartFile file);

    /** Delete one image — removes the DB row AND the physical file (if locally stored). */
    void delete(Long productId, Long imageId);

    /** All images for a product, ordered for gallery display. */
    List<EcProductImageDTO> findByProduct(Long productId);

    EcProductImageDTO findById(Long productId, Long imageId);
}
