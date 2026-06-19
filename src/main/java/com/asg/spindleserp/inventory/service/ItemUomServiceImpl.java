package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.inventory.dto.ItemUomDTO;
import com.asg.spindleserp.inventory.entity.ItemUom;
import com.asg.spindleserp.inventory.repository.ItemUomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemUomServiceImpl implements ItemUomService {

    private final ItemUomRepository uomRepository;
    private final JdbcTemplate      jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemUomDTO create(ItemUomDTO dto) {
        String code  = dto.getCode().trim().toUpperCase();

        ItemUom entity = ItemUom.builder()
                .code(code)
                .name(dto.getName().trim())
                .symbol(dto.getSymbol())
                .category(resolveCategory(dto.getCategory()))
                .isBaseUnit(Boolean.TRUE.equals(dto.getIsBaseUnit()))
                .conversionFactor(dto.getConversionFactor() != null ? dto.getConversionFactor() : BigDecimal.ONE)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return toDTO(uomRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public ItemUomDTO update(Long id, ItemUomDTO dto) {
        ItemUom entity = findEntityById(id);
        String code    = dto.getCode().trim().toUpperCase();
        entity.setCode(code);
        entity.setName(dto.getName().trim());
        entity.setSymbol(dto.getSymbol());
        entity.setCategory(resolveCategory(dto.getCategory()));
        entity.setBaseUnit(Boolean.TRUE.equals(dto.getIsBaseUnit()));
        entity.setConversionFactor(dto.getConversionFactor() != null ? dto.getConversionFactor() : entity.getConversionFactor());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(uomRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public ItemUomDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<ItemUomDTO> findAll() {
        return uomRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ItemUomDTO> findActiveByOrg(Long orgId) {
        return uomRepository.findByOrganizationIdAndActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { uomRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public ItemUomDTO toggleStatus(Long id) {
        ItemUom entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(uomRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList("u.code", "u.name", "u.symbol", "u.category"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY u.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                u.id,
                u.code,
                u.name,
                COALESCE(u.symbol, '—')                     AS symbol,
                u.category,
                u.conversion_factor,
                CASE WHEN u.is_base_unit THEN '<span class="badge bg-info text-dark">Base</span>' ELSE '' END AS base_flag,
                CASE WHEN u.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="uomShow('   || u.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="uomEdit('   || u.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="uomToggle(' || u.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="uomDelete(' || u.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM inv_item_uom u
            %s
            ORDER BY u.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public ItemUomDTO toDTO(ItemUom e) {
        return ItemUomDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .symbol(e.getSymbol())
                .category(e.getCategory() != null ? e.getCategory().name() : null)
                .isBaseUnit(e.isBaseUnit())
                .conversionFactor(e.getConversionFactor())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private ItemUom findEntityById(Long id) {
        return uomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UOM #" + id + " not found."));
    }

    private ItemUom.UomCategory resolveCategory(String cat) {
        if (cat == null || cat.isBlank()) throw new IllegalArgumentException("UOM category is required.");
        try { return ItemUom.UomCategory.valueOf(cat.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid UOM category: '" + cat + "'."); }
    }
}
