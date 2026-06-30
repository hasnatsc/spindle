// Path: com/asg/spindleserp/ecommerce/productSupport/service/EcProductImageServiceImpl.java
package com.asg.spindleserp.ecommerce.productSupport.service;

import com.asg.spindleserp.ecommerce.productSupport.dto.EcProductImageDTO;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductImage;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductCatalogRepository;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductImageRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * EcProductImageServiceImpl — orchestrates EcProductImageStorageService (disk)
 * and EcProductImageRepository (DB) for standalone, single-image operations.
 *
 * This is intentionally separate from EcProductCatalogServiceImpl.syncImages(),
 * which still handles the bulk clear-and-reinsert path used when the full
 * product form is submitted. These two paths can coexist: syncImages() is
 * for "rebuild the whole gallery from this list", while this service is for
 * "upload/replace/delete exactly one image" without disturbing the rest.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcProductImageServiceImpl implements EcProductImageService {

    private final EcProductImageRepository     imageRepository;
    private final EcProductCatalogRepository   catalogRepository;
    private final EcProductImageStorageService storageService;

    @Override
    public EcProductImageDTO upload(Long productId, MultipartFile file, String altText, Boolean isPrimary) {
        EcProductCatalog product = resolveProduct(productId);
        String url = storageService.store(productId, file);

        if (Boolean.TRUE.equals(isPrimary)) unsetExistingPrimary(productId);

        int nextOrder = (int) imageRepository.countByProductId(productId);

        EcProductImage entity = EcProductImage.builder()
                .product(product)
                .imageUrl(url)
                .altText(altText)
                .displayOrder(nextOrder)
                .isPrimary(Boolean.TRUE.equals(isPrimary) || nextOrder == 0) // first image defaults to primary
                .active(true)
                .createdBy(SecurityHelper.currentUsername().orElse("system"))
                .build();

        return toDTO(imageRepository.save(entity));
    }

    @Override
    public EcProductImageDTO addByUrl(Long productId, EcProductImageDTO dto) {
        EcProductCatalog product = resolveProduct(productId);
        if (!StringUtils.hasText(dto.getImageUrl()))
            throw new IllegalArgumentException("Image URL is required.");

        if (Boolean.TRUE.equals(dto.getIsPrimary())) unsetExistingPrimary(productId);
        int nextOrder = dto.getDisplayOrder() != null ? dto.getDisplayOrder() : (int) imageRepository.countByProductId(productId);

        EcProductImage entity = EcProductImage.builder()
                .product(product)
                .imageUrl(dto.getImageUrl().trim())
                .thumbnailUrl(dto.getThumbnailUrl())
                .altText(dto.getAltText())
                .displayOrder(nextOrder)
                .isPrimary(Boolean.TRUE.equals(dto.getIsPrimary()) || nextOrder == 0)
                .active(dto.getActive() == null || dto.getActive())
                .createdBy(SecurityHelper.currentUsername().orElse("system"))
                .build();

        return toDTO(imageRepository.save(entity));
    }

    @Override
    public EcProductImageDTO update(Long productId, Long imageId, EcProductImageDTO dto) {
        EcProductImage entity = resolveImage(productId, imageId);

        if (dto.getAltText() != null) entity.setAltText(dto.getAltText());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getActive() != null) entity.setActive(dto.getActive());

        if (Boolean.TRUE.equals(dto.getIsPrimary()) && !entity.isPrimary()) {
            unsetExistingPrimary(productId);
            entity.setPrimary(true);
        } else if (Boolean.FALSE.equals(dto.getIsPrimary())) {
            entity.setPrimary(false);
        }

        return toDTO(imageRepository.save(entity));
    }

    @Override
    public EcProductImageDTO replaceFile(Long productId, Long imageId, MultipartFile file) {
        EcProductImage entity = resolveImage(productId, imageId);
        String oldUrl = entity.getImageUrl();

        String newUrl = storageService.store(productId, file);
        entity.setImageUrl(newUrl);
        EcProductImage saved = imageRepository.save(entity);

        // Clean up the old file only after the new one is safely persisted
        storageService.delete(oldUrl);

        return toDTO(saved);
    }

    @Override
    public void delete(Long productId, Long imageId) {
        EcProductImage entity = resolveImage(productId, imageId);
        boolean wasPrimary = entity.isPrimary();
        String url = entity.getImageUrl();

        imageRepository.delete(entity);
        storageService.delete(url);

        // If we just deleted the primary image, promote the next one in order
        if (wasPrimary) {
            imageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                    .findFirst()
                    .ifPresent(next -> { next.setPrimary(true); imageRepository.save(next); });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcProductImageDTO> findByProduct(Long productId) {
        return imageRepository.findByProductIdOrderByDisplayOrderAsc(productId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EcProductImageDTO findById(Long productId, Long imageId) {
        return toDTO(resolveImage(productId, imageId));
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private EcProductCatalog resolveProduct(Long productId) {
        return catalogRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product #" + productId + " not found."));
    }

    private EcProductImage resolveImage(Long productId, Long imageId) {
        return imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Image #" + imageId + " not found for this product."));
    }

    private void unsetExistingPrimary(Long productId) {
        imageRepository.findByProductIdAndIsPrimaryTrue(productId)
                .forEach(img -> { img.setPrimary(false); imageRepository.save(img); });
    }

    private EcProductImageDTO toDTO(EcProductImage e) {
        return EcProductImageDTO.builder()
                .id(e.getId())
                .productId(e.getProduct() != null ? e.getProduct().getId() : null)
                .imageUrl(e.getImageUrl())
                .thumbnailUrl(e.getThumbnailUrl())
                .altText(e.getAltText())
                .displayOrder(e.getDisplayOrder())
                .isPrimary(e.isPrimary())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .build();
    }
}
