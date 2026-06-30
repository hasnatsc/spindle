// Path: com/asg/spindleserp/ecommerce/service/EcCategoryServiceImpl.java
package com.asg.spindleserp.ecommerce.productSupport.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.productSupport.dto.EcCategoryDTO;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcCategory;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcCategoryAttribute;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcCategoryRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcCategoryServiceImpl implements EcCategoryService {

    private final EcCategoryRepository categoryRepository;
    private final JdbcTemplate          jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────
    @Override
    public EcCategoryDTO create(EcCategoryDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = dto.getCategoryCode().trim().toUpperCase();
        String slug = dto.getSlug().trim().toLowerCase();

        if (categoryRepository.existsByOrganizationIdAndCategoryCode(orgId, code))
            throw new IllegalArgumentException("Category code '" + code + "' already exists.");
        if (categoryRepository.existsByOrganizationIdAndSlug(orgId, slug))
            throw new IllegalArgumentException("Slug '" + slug + "' already in use.");

        EcCategory parent = dto.getParentCategoryId() != null
                ? categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found."))
                : null;

        EcCategory entity = EcCategory.builder()
                .parentCategory(parent)
                .categoryCode(code)
                .categoryName(dto.getCategoryName().trim())
                .slug(slug)
                .shortDescription(dto.getShortDescription())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .bannerUrl(dto.getBannerUrl())
                .icon(dto.getIcon())
                .metaTitle(dto.getMetaTitle())
                .metaKeywords(dto.getMetaKeywords())
                .metaDescription(dto.getMetaDescription())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .levelNo(parent != null ? (parent.getLevelNo() != null ? parent.getLevelNo() + 1 : 2) : 1)
                .isMenu(dto.getIsMenu() == null || dto.getIsMenu())
                .isFeatured(Boolean.TRUE.equals(dto.getIsFeatured()))
                .active(dto.getActive() == null || dto.getActive())
                .deleted(false)
                .build();

        entity = categoryRepository.save(entity);
        syncAttributes(entity, dto.getAttributes());
        return toDTO(categoryRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public EcCategoryDTO update(Long id, EcCategoryDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcCategory entity = findEntityById(id);
        String code = dto.getCategoryCode().trim().toUpperCase();
        String slug = dto.getSlug().trim().toLowerCase();

        if (!entity.getCategoryCode().equals(code) &&
                categoryRepository.existsByOrganizationIdAndCategoryCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Category code '" + code + "' already exists.");
        if (!entity.getSlug().equals(slug) &&
                categoryRepository.existsByOrganizationIdAndSlugAndIdNot(orgId, slug, id))
            throw new IllegalArgumentException("Slug '" + slug + "' already in use.");
        if (dto.getParentCategoryId() != null && dto.getParentCategoryId().equals(id))
            throw new IllegalArgumentException("A category cannot be its own parent.");

        EcCategory parent = dto.getParentCategoryId() != null
                ? categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found."))
                : null;

        entity.setParentCategory(parent);
        entity.setCategoryCode(code);
        entity.setCategoryName(dto.getCategoryName().trim());
        entity.setSlug(slug);
        entity.setShortDescription(dto.getShortDescription());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setBannerUrl(dto.getBannerUrl());
        entity.setIcon(dto.getIcon());
        entity.setMetaTitle(dto.getMetaTitle());
        entity.setMetaKeywords(dto.getMetaKeywords());
        entity.setMetaDescription(dto.getMetaDescription());
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : entity.getDisplayOrder());
        entity.setLevelNo(parent != null ? (parent.getLevelNo() != null ? parent.getLevelNo() + 1 : 2) : 1);
        entity.setMenu(dto.getIsMenu() == null || dto.getIsMenu());
        entity.setFeatured(Boolean.TRUE.equals(dto.getIsFeatured()));
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        syncAttributes(entity, dto.getAttributes());
        return toDTO(categoryRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    @Override @Transactional(readOnly = true)
    public EcCategoryDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcCategoryDTO> findActiveByOrg(Long orgId) {
        return categoryRepository.findActiveByOrg(orgId).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<EcCategoryDTO> findAllByOrg(Long orgId) {
        return categoryRepository.findAllByOrgNotDeleted(orgId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public void delete(Long id) {
        EcCategory entity = findEntityById(id);
        if (!entity.getChildren().isEmpty())
            throw new IllegalArgumentException("Cannot delete category with sub-categories. Remove children first.");
        categoryRepository.delete(entity);
    }

    // ── TOGGLE ────────────────────────────────────────────────────────────────
    @Override
    public EcCategoryDTO toggleStatus(Long id) {
        EcCategory entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(categoryRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────
    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE c.deleted = false AND c.organization_id = "
                + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "c.category_code", "c.category_name", "c.slug",
                        "p.category_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.level_no, c.display_order, c.id)  AS sl,
                COUNT(*)     OVER ()                                             AS full_count,
                c.id,
                REPEAT('&nbsp;&nbsp;&nbsp;', c.level_no - 1) || c.category_code AS category_code,
                c.category_name,
                c.slug,
                COALESCE(p.category_name, '— Root —')                           AS parent_name,
                c.level_no,
                c.display_order,
                (SELECT COUNT(*) FROM ec_category_attributes a WHERE a.category_id = c.id AND a.active = true) AS attr_count,
                CASE WHEN c.is_featured
                    THEN '<span class="badge bg-warning text-dark">Featured</span>'
                    ELSE '<span class="badge bg-light text-secondary">—</span>'
                END AS featured_badge,
                TO_CHAR(c.created_at, 'DD-Mon-YYYY')  AS created_at,
                CASE WHEN c.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="eccShow('   || c.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="eccEdit('   || c.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="eccToggle(' || c.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="eccDelete(' || c.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                      AS actions
            FROM ec_categories c
            LEFT JOIN ec_categories p ON p.id = c.parent_category_id
            %s
            ORDER BY c.level_no, c.display_order, c.id
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    @Override
    public EcCategoryDTO toDTO(EcCategory e) {
        List<EcCategoryDTO.AttrDTO> attrs = e.getAttributes().stream().map(a ->
            EcCategoryDTO.AttrDTO.builder()
                .id(a.getId())
                .attributeName(a.getAttributeName())
                .attributeLabel(a.getAttributeLabel())
                .dataType(a.getDataType() != null ? a.getDataType().name() : null)
                .isRequired(a.isRequired())
                .searchable(a.isSearchable())
                .filterable(a.isFilterable())
                .sortable(a.isSortable())
                .displayOrder(a.getDisplayOrder())
                .active(a.isActive())
                .build()
        ).toList();

        return EcCategoryDTO.builder()
                .id(e.getId())
                .parentCategoryId(e.getParentCategory() != null ? e.getParentCategory().getId() : null)
                .parentCategoryName(e.getParentCategory() != null ? e.getParentCategory().getCategoryName() : null)
                .categoryCode(e.getCategoryCode())
                .categoryName(e.getCategoryName())
                .slug(e.getSlug())
                .shortDescription(e.getShortDescription())
                .description(e.getDescription())
                .imageUrl(e.getImageUrl())
                .bannerUrl(e.getBannerUrl())
                .icon(e.getIcon())
                .metaTitle(e.getMetaTitle())
                .metaKeywords(e.getMetaKeywords())
                .metaDescription(e.getMetaDescription())
                .displayOrder(e.getDisplayOrder())
                .levelNo(e.getLevelNo())
                .isMenu(e.isMenu())
                .isFeatured(e.isFeatured())
                .active(e.isActive())
                .attributes(attrs)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .build();
    }

    // ── SYNC ATTRIBUTES ───────────────────────────────────────────────────────
    private void syncAttributes(EcCategory entity, List<EcCategoryDTO.AttrDTO> dtoList) {
        entity.getAttributes().clear();
        if (dtoList == null || dtoList.isEmpty()) return;
        dtoList.forEach(d -> {
            if (d.getAttributeName() == null || d.getAttributeName().isBlank()) return;
            EcCategoryAttribute.DataType dt;
            try { dt = EcCategoryAttribute.DataType.valueOf(d.getDataType() != null ? d.getDataType() : "TEXT"); }
            catch (IllegalArgumentException ex) { dt = EcCategoryAttribute.DataType.TEXT; }
            entity.getAttributes().add(EcCategoryAttribute.builder()
                    .category(entity)
                    .attributeName(d.getAttributeName().trim())
                    .attributeLabel(d.getAttributeLabel())
                    .dataType(dt)
                    .isRequired(Boolean.TRUE.equals(d.getIsRequired()))
                    .searchable(d.getSearchable() == null || d.getSearchable())
                    .filterable(d.getFilterable() == null || d.getFilterable())
                    .sortable(Boolean.TRUE.equals(d.getSortable()))
                    .displayOrder(d.getDisplayOrder() != null ? d.getDisplayOrder() : 0)
                    .active(d.getActive() == null || d.getActive())
                    .build());
        });
    }

    private EcCategory findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EC Category #" + id + " not found."));
    }
}
