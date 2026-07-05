// Path: com/asg/spindleserp/ecommerce/service/EcProductCatalogServiceImpl.java
package com.asg.spindleserp.ecommerce.productSupport.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.productSupport.dto.EcProductCatalogDTO;
import com.asg.spindleserp.ecommerce.productSupport.entity.*;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductCatalogRepository;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.inventory.repository.ItemRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcProductCatalogServiceImpl implements EcProductCatalogService {

    private final EcProductCatalogRepository   catalogRepository;
    private final ItemRepository               itemRepository;
    private final JdbcTemplate                 jdbcTemplate;
    private final EcProductImageStorageService storageService;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public EcProductCatalogDTO create(EcProductCatalogDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String slug = dto.getSlug().trim().toLowerCase();

        if (catalogRepository.existsByOrganizationIdAndSlug(orgId, slug))
            throw new IllegalArgumentException("Slug '" + slug + "' is already in use.");

        if (catalogRepository.existsByOrganizationIdAndItemId(orgId, dto.getItemId()))
            throw new IllegalArgumentException("A catalog entry already exists for this item.");

        Item item = resolveItem(dto.getItemId());

        EcProductCatalog entity = EcProductCatalog.builder()
                .item(item)
                .category(dto.getCategoryId() != null ? resolveCategoryRef(dto.getCategoryId()) : null)
                .slug(slug)
                .productTitle(dto.getProductTitle().trim())
                .shortDescription(dto.getShortDescription())
                .description(dto.getDescription())
                .seoTitle(dto.getSeoTitle())
                .seoKeywords(dto.getSeoKeywords())
                .seoDescription(dto.getSeoDescription())
                .youtubeVideo(dto.getYoutubeVideo())
                .warrantyInformation(dto.getWarrantyInformation())
                .returnPolicy(dto.getReturnPolicy())
                .shippingInformation(dto.getShippingInformation())
                .minimumOrderQty(dto.getMinimumOrderQty())
                .maximumOrderQty(dto.getMaximumOrderQty())
                .featured(Boolean.TRUE.equals(dto.getFeatured()))
                .bestSeller(Boolean.TRUE.equals(dto.getBestSeller()))
                .trending(Boolean.TRUE.equals(dto.getTrending()))
                .newArrival(Boolean.TRUE.equals(dto.getNewArrival()))
                .recommended(Boolean.TRUE.equals(dto.getRecommended()))
                .published(Boolean.TRUE.equals(dto.getPublished()))
                .publishDate(dto.getPublishDate())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .deleted(false)
                .build();

        // Persist first to get PK, then sync children
        entity = catalogRepository.save(entity);
        if (dto.getImages() != null) syncImages(entity, dto.getImages());
        syncVariants(entity, dto.getVariants());
        syncAttrValues(entity, dto.getAttrValues());
        return toDTO(catalogRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public EcProductCatalogDTO update(Long id, EcProductCatalogDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcProductCatalog entity = findEntityById(id);
        String slug = dto.getSlug().trim().toLowerCase();

        if (!entity.getSlug().equals(slug) && catalogRepository.existsByOrganizationIdAndSlugAndIdNot(orgId, slug, id))
            throw new IllegalArgumentException("Slug '" + slug + "' is already in use.");

        if (entity.getItem().getId() != null && !entity.getItem().getId().equals(dto.getItemId()) && catalogRepository.existsByOrganizationIdAndItemIdAndIdNot(orgId, dto.getItemId(), id))
            throw new IllegalArgumentException("A catalog entry already exists for the selected item.");

        entity.setItem(resolveItem(dto.getItemId()));
        entity.setCategory(dto.getCategoryId() != null ? resolveCategoryRef(dto.getCategoryId()) : null);
        entity.setSlug(slug);
        entity.setProductTitle(dto.getProductTitle().trim());
        entity.setShortDescription(dto.getShortDescription());
        entity.setDescription(dto.getDescription());
        entity.setSeoTitle(dto.getSeoTitle());
        entity.setSeoKeywords(dto.getSeoKeywords());
        entity.setSeoDescription(dto.getSeoDescription());
        entity.setYoutubeVideo(dto.getYoutubeVideo());
        entity.setWarrantyInformation(dto.getWarrantyInformation());
        entity.setReturnPolicy(dto.getReturnPolicy());
        entity.setShippingInformation(dto.getShippingInformation());
        entity.setMinimumOrderQty(dto.getMinimumOrderQty());
        entity.setMaximumOrderQty(dto.getMaximumOrderQty());
        entity.setFeatured(Boolean.TRUE.equals(dto.getFeatured()));
        entity.setBestSeller(Boolean.TRUE.equals(dto.getBestSeller()));
        entity.setTrending(Boolean.TRUE.equals(dto.getTrending()));
        entity.setNewArrival(Boolean.TRUE.equals(dto.getNewArrival()));
        entity.setRecommended(Boolean.TRUE.equals(dto.getRecommended()));
        entity.setPublished(Boolean.TRUE.equals(dto.getPublished()));
        entity.setPublishDate(dto.getPublishDate());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        // ★ FIX: images == null means "not managed by this request" — the form
        // intentionally omits images because uploads go through the dedicated
        // /ecommerce/products/{id}/images/** endpoints. Previously a null list
        // cleared the collection and orphanRemoval DELETED every image row on
        // every product update. Only sync when the caller explicitly sends a list.
        if (dto.getImages() != null)     syncImages(entity, dto.getImages());
        syncVariants(entity, dto.getVariants());
        syncAttrValues(entity, dto.getAttrValues());
        return toDTO(catalogRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public EcProductCatalogDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override @Transactional(readOnly = true)
    public List<EcProductCatalogDTO> findActiveByOrg(Long orgId) {
        return catalogRepository.findByOrganizationIdAndActiveTrue(orgId)
                .stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        EcProductCatalog entity = findEntityById(id);
        // Capture URLs BEFORE the cascade delete removes the rows, then clean
        // the physical files after the DB delete succeeds (external CDN URLs
        // are ignored by the storage service — only /uploads/** files are touched).
        List<String> fileUrls = entity.getImages().stream()
                .map(EcProductImage::getImageUrl)
                .toList();
        catalogRepository.delete(entity);
        storageService.deleteAll(fileUrls);
    }

    // ── TOGGLE STATUS ─────────────────────────────────────────────────────────

    @Override
    public EcProductCatalogDTO toggleStatus(Long id) {
        EcProductCatalog entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(catalogRepository.save(entity));
    }

    // ── TOGGLE PUBLISHED ──────────────────────────────────────────────────────

    @Override
    public EcProductCatalogDTO togglePublished(Long id) {
        EcProductCatalog entity = findEntityById(id);
        entity.setPublished(!entity.isPublished());
        return toDTO(catalogRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE p.deleted = false "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "p.slug", "p.product_title",
                        "i.item_code", "i.item_name", "i.sku",
                        "ec.category_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY p.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                p.id,
                p.product_title,
                p.slug,
                i.item_code,
                i.item_name,
                COALESCE(ec.category_name, '—')              AS category_name,
                COALESCE(img_count.cnt::text, '0')           AS image_count,
                COALESCE(var_count.cnt::text, '0')           AS variant_count,
                CASE WHEN p.featured
                    THEN '<span class="badge bg-warning text-dark">Featured</span>'
                    ELSE '<span class="badge bg-light text-secondary">—</span>'
                END AS featured_badge,
                CASE WHEN p.published
                    THEN '<span class="badge bg-success">Published</span>'
                    ELSE '<span class="badge bg-secondary">Draft</span>'
                END AS publish_status,
                TO_CHAR(p.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN p.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ecpShow('   || p.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ecpEdit('   || p.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="ecpTogglePublish(' || p.id || ')" class="btn btn-white btn-sm" title="Publish/Draft"><i class="fa-regular fa-newspaper text-info"></i></a>'
                    || '<a href="javascript:;" onclick="ecpToggle(' || p.id || ')" class="btn btn-white btn-sm" title="Toggle Status"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="ecpDelete(' || p.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM ec_product_catalog p
            JOIN inv_items          i   ON i.id  = p.item_id
            LEFT JOIN ec_categories ec  ON ec.id = p.category_id
            LEFT JOIN (SELECT product_id, COUNT(*) AS cnt FROM ec_product_images   GROUP BY product_id) img_count ON img_count.product_id = p.id
            LEFT JOIN (SELECT product_id, COUNT(*) AS cnt FROM ec_product_variants  GROUP BY product_id) var_count ON var_count.product_id = p.id
            %s
            ORDER BY p.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public EcProductCatalogDTO toDTO(EcProductCatalog e) {
        // ── Images ────────────────────────────────────────────────────────
        List<EcProductCatalogDTO.ImageDTO> images = e.getImages().stream().map(img ->
            EcProductCatalogDTO.ImageDTO.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .thumbnailUrl(img.getThumbnailUrl())
                .altText(img.getAltText())
                .displayOrder(img.getDisplayOrder())
                .isPrimary(img.isPrimary())
                .active(img.isActive())
                .build()
        ).toList();

        // ── Variants ──────────────────────────────────────────────────────
        List<EcProductCatalogDTO.VariantDTO> variants = e.getVariants().stream().map(v ->
            EcProductCatalogDTO.VariantDTO.builder()
                .id(v.getId())
                .itemId(v.getItem()     != null ? v.getItem().getId()       : null)
                .itemCode(v.getItem()   != null ? v.getItem().getItemCode() : null)
                .itemName(v.getItem()   != null ? v.getItem().getItemName() : null)
                .variantCode(v.getVariantCode())
                .variantName(v.getVariantName())
                .color(v.getColor())
                .sizeName(v.getSizeName())
                .weightOverride(v.getWeightOverride())
                .lengthOverride(v.getLengthOverride())
                .widthOverride(v.getWidthOverride())
                .heightOverride(v.getHeightOverride())
                .sellingPrice(v.getSellingPrice())
                .comparePrice(v.getComparePrice())
                .active(v.isActive())
                .build()
        ).toList();

        // ── Attribute values ──────────────────────────────────────────────
        List<EcProductCatalogDTO.AttrValueDTO> attrValues = e.getAttributeValues().stream().map(av ->
            EcProductCatalogDTO.AttrValueDTO.builder()
                .id(av.getId())
                .categoryAttributeId(av.getCategoryAttribute() != null ? av.getCategoryAttribute().getId() : null)
                .attributeName(av.getCategoryAttribute()  != null ? av.getCategoryAttribute().getAttributeName()  : null)
                .attributeLabel(av.getCategoryAttribute() != null ? av.getCategoryAttribute().getAttributeLabel() : null)
                .dataType(av.getCategoryAttribute()       != null ? av.getCategoryAttribute().getDataType().name() : null)
                .attributeValue(av.getAttributeValue())
                .build()
        ).toList();

        return EcProductCatalogDTO.builder()
                .id(e.getId())
                .itemId(e.getItem()      != null ? e.getItem().getId()          : null)
                .itemCode(e.getItem()    != null ? e.getItem().getItemCode()     : null)
                .itemName(e.getItem()    != null ? e.getItem().getItemName()     : null)
                .itemSku(e.getItem()     != null ? e.getItem().getSku()          : null)
                .itemUom(e.getItem()     != null ? e.getItem().getUnitOfMeasure() : null)
                .itemUnitPrice(e.getItem() != null ? e.getItem().getUnitPrice()  : null)
                .categoryId(e.getCategory()   != null ? e.getCategory().getId()           : null)
                .categoryName(e.getCategory() != null ? e.getCategory().getCategoryName() : null)
                .slug(e.getSlug())
                .productTitle(e.getProductTitle())
                .shortDescription(e.getShortDescription())
                .description(e.getDescription())
                .seoTitle(e.getSeoTitle())
                .seoKeywords(e.getSeoKeywords())
                .seoDescription(e.getSeoDescription())
                .youtubeVideo(e.getYoutubeVideo())
                .warrantyInformation(e.getWarrantyInformation())
                .returnPolicy(e.getReturnPolicy())
                .shippingInformation(e.getShippingInformation())
                .minimumOrderQty(e.getMinimumOrderQty())
                .maximumOrderQty(e.getMaximumOrderQty())
                .featured(e.isFeatured())
                .bestSeller(e.isBestSeller())
                .trending(e.isTrending())
                .newArrival(e.isNewArrival())
                .recommended(e.isRecommended())
                .published(e.isPublished())
                .publishDate(e.getPublishDate())
                .active(e.isActive())
                .deleted(e.isDeleted())
                .images(images)
                .variants(variants)
                .attrValues(attrValues)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ── SYNC HELPERS (clear-and-reinsert, CascadeType.ALL + orphanRemoval) ────

    private void syncImages(EcProductCatalog entity,
                            List<EcProductCatalogDTO.ImageDTO> dtoList) {
        entity.getImages().clear();
        if (dtoList == null || dtoList.isEmpty()) return;
        dtoList.forEach(d -> entity.getImages().add(
            EcProductImage.builder()
                .product(entity)
                .imageUrl(d.getImageUrl() != null ? d.getImageUrl().trim() : "")
                .thumbnailUrl(d.getThumbnailUrl())
                .altText(d.getAltText())
                .displayOrder(d.getDisplayOrder() != null ? d.getDisplayOrder() : 0)
                .isPrimary(Boolean.TRUE.equals(d.getIsPrimary()))
                .active(d.getActive() == null || d.getActive())
                .build()
        ));
    }

    private void syncVariants(EcProductCatalog entity, List<EcProductCatalogDTO.VariantDTO> dtoList) {
        List<EcProductCatalogDTO.VariantDTO> incoming = dtoList == null ? List.of() : dtoList;
        incoming.forEach(d -> {
            if (d.getItemId() == null)
                throw new IllegalArgumentException("Each variant must be linked to an inventory item.");
        });

        Map<Long, EcProductVariant> existing = entity.getVariants().stream()
                .collect(Collectors.toMap(v -> v.getItem().getId(), v -> v, (a, b) -> a));

        Set<Long> incomingItemIds = incoming.stream()
                .map(EcProductCatalogDTO.VariantDTO::getItemId).collect(Collectors.toSet());

        entity.getVariants().removeIf(v -> !incomingItemIds.contains(v.getItem().getId()));

        for (var d : incoming) {
            EcProductVariant v = existing.get(d.getItemId());
            if (v == null) {
                v = EcProductVariant.builder()
                        .product(entity)
                        .item(resolveItem(d.getItemId()))
                        .deleted(false)
                        .build();
                entity.getVariants().add(v);
            }
            v.setVariantCode(d.getVariantCode());
            v.setVariantName(d.getVariantName());
            v.setColor(d.getColor());
            v.setSizeName(d.getSizeName());
            v.setWeightOverride(d.getWeightOverride());
            v.setLengthOverride(d.getLengthOverride());
            v.setWidthOverride(d.getWidthOverride());
            v.setHeightOverride(d.getHeightOverride());
            v.setSellingPrice(d.getSellingPrice());
            v.setComparePrice(d.getComparePrice());
            v.setActive(d.getActive() == null || d.getActive());
        }
    }

    private void syncAttrValues(EcProductCatalog entity, List<EcProductCatalogDTO.AttrValueDTO> dtoList) {
        List<EcProductCatalogDTO.AttrValueDTO> incoming =
                dtoList == null ? List.of()
                        : dtoList.stream().filter(d -> d.getCategoryAttributeId() != null).toList();

        Map<Long, EcProductAttributeValue> existing = entity.getAttributeValues().stream()
                .collect(Collectors.toMap(av -> av.getCategoryAttribute().getId(), av -> av));

        Set<Long> incomingIds = incoming.stream()
                .map(EcProductCatalogDTO.AttrValueDTO::getCategoryAttributeId)
                .collect(Collectors.toSet());

        // remove attrs no longer present (orphanRemoval deletes them)
        entity.getAttributeValues().removeIf(av -> !incomingIds.contains(av.getCategoryAttribute().getId()));

        for (var d : incoming) {
            EcProductAttributeValue av = existing.get(d.getCategoryAttributeId());
            if (av != null) {
                av.setAttributeValue(d.getAttributeValue());          // update in place
            } else {
                entity.getAttributeValues().add(                      // genuinely new
                        EcProductAttributeValue.builder()
                                .product(entity)
                                .categoryAttribute(resolveCategoryAttribute(d.getCategoryAttributeId()))
                                .attributeValue(d.getAttributeValue())
                                .build());
            }
        }
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private EcProductCatalog findEntityById(Long id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product catalog #" + id + " not found."));
    }

    private Item resolveItem(Long itemId) {
        if (itemId == null) throw new IllegalArgumentException("Item is required.");
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item #" + itemId + " not found."));
    }

    /**
     * Proxy reference for FK assignment — Hibernate resolves the FK without a SELECT.
     * Existence is implicitly validated by the DB FK constraint on ec_product_catalog.category_id.
     */
    private EcCategory resolveCategoryRef(Long catId) {
        EcCategory ref = new EcCategory();
        ref.setId(catId);
        return ref;
    }

    private EcCategoryAttribute resolveCategoryAttribute(Long attrId) {
        EcCategoryAttribute ref = new EcCategoryAttribute();
        ref.setId(attrId);
        return ref;
    }

    private static Long toLong(Object v) { return v == null ? 0L : Long.parseLong(v.toString()); }
    private static BigDecimal toBD(Object v) { return v == null ? null : new BigDecimal(v.toString()); }
}
