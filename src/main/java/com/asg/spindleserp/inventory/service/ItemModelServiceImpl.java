package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.inventory.dto.ItemModelDTO;
import com.asg.spindleserp.inventory.entity.ItemBrand;
import com.asg.spindleserp.inventory.entity.ItemModel;
import com.asg.spindleserp.inventory.repository.ItemBrandRepository;
import com.asg.spindleserp.inventory.repository.ItemModelRepository;
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
public class ItemModelServiceImpl implements ItemModelService {

    private final ItemModelRepository modelRepository;
    private final ItemBrandRepository brandRepository;
    private final JdbcTemplate        jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemModelDTO create(ItemModelDTO dto) {
        String code    = dto.getModelCode().trim().toUpperCase();
        Long   brandId = dto.getBrandId();

        ItemBrand brand = brandId != null ? brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand #" + brandId + " not found.")) : null;

        ItemModel entity = ItemModel.builder()
                .brand(brand)
                .modelCode(code)
                .modelName(dto.getModelName().trim())
                .description(dto.getDescription())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return toDTO(modelRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemModelDTO update(Long id, ItemModelDTO dto) {
        ItemModel entity = findEntityById(id);
        String code      = dto.getModelCode().trim().toUpperCase();
        Long   brandId   = dto.getBrandId();

        ItemBrand brand = brandId != null ? brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand #" + brandId + " not found.")) : null;

        entity.setBrand(brand);
        entity.setModelCode(code);
        entity.setModelName(dto.getModelName().trim());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(modelRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public ItemModelDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<ItemModelDTO> findAll() {
        return modelRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemModelDTO> findActiveByOrg(Long orgId) {
        return modelRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemModelDTO> findActiveByBrand(Long brandId) {
        return modelRepository.findByBrandIdAndIsActiveTrue(brandId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { modelRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public ItemModelDTO toggleStatus(Long id) {
        ItemModel entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(modelRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE m.organization_id = "+ ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "m.model_code", "m.model_name", "b.brand_name", "b.brand_code"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY m.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                m.id,
                m.model_code,
                m.model_name,
                COALESCE(b.brand_name, '—')                 AS brand_name,
                COALESCE(LEFT(m.description, 80), '—')      AS description,
                TO_CHAR(m.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN m.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="modelShow('   || m.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="modelEdit('   || m.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="modelToggle(' || m.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="modelDelete(' || m.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM inv_item_models m
            LEFT JOIN inv_item_brands b ON b.id = m.brand_id
            %s
            ORDER BY m.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public ItemModelDTO toDTO(ItemModel e) {
        return ItemModelDTO.builder()
                .id(e.getId())
                .brandId(e.getBrand()   != null ? e.getBrand().getId()       : null)
                .brandName(e.getBrand() != null ? e.getBrand().getBrandName() : null)
                .modelCode(e.getModelCode())
                .modelName(e.getModelName())
                .description(e.getDescription())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private ItemModel findEntityById(Long id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model #" + id + " not found."));
    }
}
