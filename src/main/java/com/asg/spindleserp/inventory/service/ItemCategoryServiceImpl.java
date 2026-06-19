package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.inventory.dto.ItemCategoryDTO;
import com.asg.spindleserp.inventory.entity.ItemCategory;
import com.asg.spindleserp.inventory.repository.ItemCategoryRepository;
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
public class ItemCategoryServiceImpl implements ItemCategoryService {

    private final ItemCategoryRepository categoryRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemCategoryDTO create(ItemCategoryDTO dto) {
        String code  = dto.getCategoryCode().trim().toUpperCase();
        ItemCategory entity = ItemCategory.builder()
                .categoryCode(code)
                .categoryName(dto.getCategoryName().trim())
                .description(dto.getDescription())
                .itemType(resolveItemType(dto.getItemType()))
                .layerType(resolveLayerType(dto.getLayerType()))
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();

        if (dto.getParentCategoryId() != null)
            entity.setParentCategory(findEntityById(dto.getParentCategoryId()));

        return toDTO(categoryRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemCategoryDTO update(Long id, ItemCategoryDTO dto) {
        ItemCategory entity = findEntityById(id);
        String code         = dto.getCategoryCode().trim().toUpperCase();

        entity.setCategoryCode(code);
        entity.setCategoryName(dto.getCategoryName().trim());
        entity.setDescription(dto.getDescription());
        entity.setItemType(resolveItemType(dto.getItemType()));
        entity.setLayerType(resolveLayerType(dto.getLayerType()));
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        if (dto.getParentCategoryId() != null && !dto.getParentCategoryId().equals(id))
            entity.setParentCategory(findEntityById(dto.getParentCategoryId()));
        else
            entity.setParentCategory(null);

        return toDTO(categoryRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public ItemCategoryDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<ItemCategoryDTO> findAll() {
        return categoryRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemCategoryDTO> findActiveByOrg(Long orgId) {
        return categoryRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemCategoryDTO> findParentCandidates(Long excludeId) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.isActive() && !c.getId().equals(excludeId))
                .map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        if (categoryRepository.existsByParentCategoryId(id))
            throw new IllegalArgumentException("Cannot delete: child categories exist.");
        categoryRepository.delete(findEntityById(id));
    }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public ItemCategoryDTO toggleStatus(Long id) {
        ItemCategory entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(categoryRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "c.category_code", "c.category_name", "c.item_type", "c.layer_type", "p.category_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                c.id,
                c.category_code,
                c.category_name,
                COALESCE(c.item_type, '—')                  AS item_type,
                c.layer_type,
                COALESCE(p.category_name, '—')              AS parent_name,
                TO_CHAR(c.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN c.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="catShow('   || c.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="catEdit('   || c.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="catToggle(' || c.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="catDelete(' || c.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM inv_item_categories c
            LEFT JOIN inv_item_categories p ON p.id = c.parent_category_id
            %s
            ORDER BY c.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public ItemCategoryDTO toDTO(ItemCategory e) {
        return ItemCategoryDTO.builder()
                .id(e.getId())
                .parentCategoryId(e.getParentCategory()   != null ? e.getParentCategory().getId()            : null)
                .parentCategoryName(e.getParentCategory() != null ? e.getParentCategory().getCategoryName() : null)
                .categoryCode(e.getCategoryCode())
                .categoryName(e.getCategoryName())
                .description(e.getDescription())
                .itemType(e.getItemType()   != null ? e.getItemType().name()   : null)
                .layerType(e.getLayerType() != null ? e.getLayerType().name() : "ITEM")
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private ItemCategory findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category #" + id + " not found."));
    }

    private ItemType resolveItemType(String type) {
        if (type == null || type.isBlank()) return null;
        try { return ItemType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid item type: '" + type + "'."); }
    }

    private ItemCategory.LayerType resolveLayerType(String layer) {
        if (layer == null || layer.isBlank()) return ItemCategory.LayerType.ITEM;
        try { return ItemCategory.LayerType.valueOf(layer.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return ItemCategory.LayerType.ITEM; }
    }
}
