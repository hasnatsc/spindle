package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.setup.dto.HsCodeDTO;
import com.asg.spindleserp.setup.entity.HsCode;
import com.asg.spindleserp.setup.repository.HsCodeRepository;
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
public class HsCodeServiceImpl implements HsCodeService {

    private final HsCodeRepository hsCodeRepository;
    private final JdbcTemplate     jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public HsCodeDTO create(HsCodeDTO dto) {
        Long orgId   = dto.getOrganizationId();
        String code  = dto.getHsCode().trim();
        if (hsCodeRepository.existsByOrganizationIdAndHsCode(orgId, code)) throw new IllegalArgumentException("HS Code '" + code + "' already exists in this organisation.");

        HsCode entity = HsCode.builder()
                .organizationId(orgId)
                .hsCode(code)
                .description(dto.getDescription().trim())
                .shortDescription(dto.getShortDescription())
                .hsType(resolveHsType(dto.getHsType()))
                .vatPercent(dto.getVatPercent())
                .customsDutyPercent(dto.getCustomsDutyPercent())
                .supplementaryDutyPercent(dto.getSupplementaryDutyPercent())
                .aitPercent(dto.getAitPercent())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .isBondedAllowed(Boolean.TRUE.equals(dto.getBondedAllowed()))
                .requiresImportPermit(Boolean.TRUE.equals(dto.getRequiresImportPermit()))
                .requiresExportPermit(Boolean.TRUE.equals(dto.getRequiresExportPermit()))
                .build();
        return toDTO(hsCodeRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public HsCodeDTO update(Long id, HsCodeDTO dto) {
        HsCode entity = findEntityById(id);
        Long orgId    = dto.getOrganizationId();
        String code   = dto.getHsCode().trim();

        if (!entity.getHsCode().equals(code) && hsCodeRepository.existsByOrganizationIdAndHsCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("HS Code '" + code + "' already exists in this organisation.");

        entity.setOrganizationId(orgId);
        entity.setHsCode(code);
        entity.setDescription(dto.getDescription().trim());
        entity.setShortDescription(dto.getShortDescription());
        entity.setHsType(resolveHsType(dto.getHsType()));
        entity.setVatPercent(dto.getVatPercent());
        entity.setCustomsDutyPercent(dto.getCustomsDutyPercent());
        entity.setSupplementaryDutyPercent(dto.getSupplementaryDutyPercent());
        entity.setAitPercent(dto.getAitPercent());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        entity.setBondedAllowed(Boolean.TRUE.equals(dto.getBondedAllowed()));
        entity.setRequiresImportPermit(Boolean.TRUE.equals(dto.getRequiresImportPermit()));
        entity.setRequiresExportPermit(Boolean.TRUE.equals(dto.getRequiresExportPermit()));
        return toDTO(hsCodeRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public HsCodeDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<HsCodeDTO> findAll() {
        return hsCodeRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<HsCodeDTO> findActiveByOrg(Long orgId) {
        return hsCodeRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { hsCodeRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public HsCodeDTO toggleStatus(Long id) {
        HsCode entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(hsCodeRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "h.hs_code", "h.description", "h.short_description", "h.hs_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY h.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                h.id,
                h.hs_code,
                h.description,
                COALESCE(h.short_description, '—')          AS short_description,
                '<span class="badge bg-info text-dark">' || h.hs_type || '</span>' AS hs_type,
                COALESCE(h.vat_percent::text,  '—')         AS vat_percent,
                COALESCE(h.customs_duty_percent::text, '—') AS customs_duty_percent,
                TO_CHAR(h.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN h.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="hsShow('   || h.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="hsEdit('   || h.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="hsToggle(' || h.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="hsDelete(' || h.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM com_hs_codes h
            %s
            ORDER BY h.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public HsCodeDTO toDTO(HsCode e) {
        return HsCodeDTO.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .hsCode(e.getHsCode())
                .description(e.getDescription())
                .shortDescription(e.getShortDescription())
                .hsType(e.getHsType() != null ? e.getHsType().name() : "BOTH")
                .vatPercent(e.getVatPercent())
                .customsDutyPercent(e.getCustomsDutyPercent())
                .supplementaryDutyPercent(e.getSupplementaryDutyPercent())
                .aitPercent(e.getAitPercent())
                .active(e.isActive())
                .bondedAllowed(e.isBondedAllowed())
                .requiresImportPermit(e.isRequiresImportPermit())
                .requiresExportPermit(e.isRequiresExportPermit())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private HsCode findEntityById(Long id) {
        return hsCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("HS Code #" + id + " not found."));
    }

    private HsCode.HsType resolveHsType(String type) {
        if (type == null || type.isBlank()) return HsCode.HsType.BOTH;
        try { return HsCode.HsType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid HS Type: '" + type + "'.");
        }
    }
}
