package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.inventory.dto.ItemBrandDTO;
import com.asg.spindleserp.inventory.entity.ItemBrand;
import com.asg.spindleserp.inventory.repository.ItemBrandRepository;
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
public class ItemBrandServiceImpl implements ItemBrandService {

    private final ItemBrandRepository brandRepository;
    private final JdbcTemplate        jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemBrandDTO create(ItemBrandDTO dto) {
        String code  = dto.getBrandCode().trim().toUpperCase();
        ItemBrand entity = ItemBrand.builder()
                .brandCode(code)
                .brandName(dto.getBrandName().trim())
                .description(dto.getDescription())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return toDTO(brandRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemBrandDTO update(Long id, ItemBrandDTO dto) {
        ItemBrand entity = findEntityById(id);
        String code      = dto.getBrandCode().trim().toUpperCase();

        entity.setBrandCode(code);
        entity.setBrandName(dto.getBrandName().trim());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(brandRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public ItemBrandDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<ItemBrandDTO> findAll() {
        return brandRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemBrandDTO> findActiveByOrg(Long orgId) {
        return brandRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { brandRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public ItemBrandDTO toggleStatus(Long id) {
        ItemBrand entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(brandRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList("b.brand_code", "b.brand_name"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY b.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                b.id,
                b.brand_code,
                b.brand_name,
                COALESCE(LEFT(b.description, 80), '—')      AS description,
                TO_CHAR(b.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN b.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="brandShow('   || b.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="brandEdit('   || b.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="brandToggle(' || b.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="brandDelete(' || b.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM inv_item_brands b
            %s
            ORDER BY b.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public ItemBrandDTO toDTO(ItemBrand e) {
        return ItemBrandDTO.builder()
                .id(e.getId())
                .brandCode(e.getBrandCode())
                .brandName(e.getBrandName())
                .description(e.getDescription())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private ItemBrand findEntityById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand #" + id + " not found."));
    }
}
