package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.dto.CostCenterDTO;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.CostCenterRepository;
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
public class CostCenterServiceImpl implements CostCenterService {

    private final CostCenterRepository   costCenterRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CostCenterDTO create(CostCenterDTO dto) {
        String code = dto.getCostCenterCode().trim().toUpperCase();
        if (costCenterRepository.existsByCostCenterCode(code)) {
            throw new IllegalArgumentException("Cost center code '" + code + "' already exists.");
        }
        BusinessUnit bu = resolveBu(dto.getBusinessUnitId());

        CostCenter entity = CostCenter.builder()
                .businessUnit(bu)
                .costCenterCode(code)
                .costCenterName(dto.getCostCenterName().trim())
                .costCenterType(resolveType(dto.getCostCenterType()))
                .description(dto.getDescription())
                .managerName(dto.getManagerName())
                .managerEmail(dto.getManagerEmail())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();

        if (dto.getParentCostCenterId() != null) {
            entity.setParentCostCenter(findEntityById(dto.getParentCostCenterId()));
        }

        return toDTO(costCenterRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CostCenterDTO update(Long id, CostCenterDTO dto) {
        CostCenter entity = findEntityById(id);
        BusinessUnit bu   = resolveBu(dto.getBusinessUnitId());
        String code = dto.getCostCenterCode().trim().toUpperCase();

        boolean codeChanged = !entity.getCostCenterCode().equalsIgnoreCase(code);
        if (codeChanged && costCenterRepository.existsByCostCenterCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("Cost center code '" + code + "' already exists.");
        }

        entity.setBusinessUnit(bu);
        entity.setCostCenterCode(code);
        entity.setCostCenterName(dto.getCostCenterName().trim());
        entity.setCostCenterType(resolveType(dto.getCostCenterType()));
        entity.setDescription(dto.getDescription());
        entity.setManagerName(dto.getManagerName());
        entity.setManagerEmail(dto.getManagerEmail());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        // Parent — guard against self-reference
        if (dto.getParentCostCenterId() != null && !dto.getParentCostCenterId().equals(id)) {
            entity.setParentCostCenter(findEntityById(dto.getParentCostCenterId()));
        } else {
            entity.setParentCostCenter(null);
        }

        return toDTO(costCenterRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CostCenterDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterDTO> findAll() {
        return costCenterRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterDTO> findActiveByOrg(Long orgId) {
        return costCenterRepository.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterDTO> findActiveByBusinessUnit(Long buId) {
        return costCenterRepository.findByBusinessUnitIdAndIsActiveTrueOrderByCostCenterName(buId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterDTO> findParentCandidates(Long excludeId) {
        return costCenterRepository.findAll().stream()
                .filter(c -> c.isActive() && !c.getId().equals(excludeId))
                .map(this::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        if (costCenterRepository.existsByParentCostCenterId(id)) {
            throw new IllegalArgumentException("Cannot delete: child cost centers exist.");
        }
        costCenterRepository.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CostCenterDTO toggleStatus(Long id) {
        CostCenter entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(costCenterRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE  (server-side, JdbcTemplate — mirrors BusinessUnitService)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {

        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "cc.cost_center_code", "cc.cost_center_name",
                        "cc.manager_name", "bu.name", "cc.cost_center_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY cc.id DESC)       AS sl,
                COUNT(*)     OVER ()                          AS full_count,
                cc.id,
                cc.cost_center_code                           AS code,
                cc.cost_center_name                           AS name,
                COALESCE(
                    '<span class="badge bg-info text-dark">' || cc.cost_center_type || '</span>',
                    '—')                                      AS type,
                bu.name                                       AS business_unit,
                COALESCE(p.cost_center_name, '—')            AS parent,
                COALESCE(cc.manager_name, '—')               AS manager,
                TO_CHAR(cc.created_at, 'DD-Mon-YYYY')        AS created_at,
                CASE WHEN cc.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ccShow('   || cc.id || ')" class="btn btn-white btn-sm" title="View">'
                    ||     '<i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ccEdit('   || cc.id || ')" class="btn btn-white btn-sm" title="Edit">'
                    ||     '<i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="ccToggle(' || cc.id || ')" class="btn btn-white btn-sm" title="Toggle Status">'
                    ||     '<i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="ccDelete(' || cc.id || ')" class="btn btn-white btn-sm" title="Delete">'
                    ||     '<i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                               AS actions
            FROM  org_cost_centers cc
            JOIN  org_business_units bu   ON bu.id  = cc.business_unit_id
            LEFT  JOIN org_cost_centers p ON p.id   = cc.parent_cost_center_id
            %s
            ORDER BY cc.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));

        return DataTableResponse.of(draw, total, total, rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CostCenterDTO toDTO(CostCenter e) {
        return CostCenterDTO.builder()
                .id(e.getId())
                .businessUnitId(e.getBusinessUnit() != null ? e.getBusinessUnit().getId()   : null)
                .businessUnitName(e.getBusinessUnit() != null ? e.getBusinessUnit().getName() : null)
                .parentCostCenterId(e.getParentCostCenter()   != null ? e.getParentCostCenter().getId()              : null)
                .parentCostCenterName(e.getParentCostCenter() != null ? e.getParentCostCenter().getCostCenterName() : null)
                .costCenterCode(e.getCostCenterCode())
                .costCenterName(e.getCostCenterName())
                .costCenterType(e.getCostCenterType() != null ? e.getCostCenterType().name() : null)
                .description(e.getDescription())
                .managerName(e.getManagerName())
                .managerEmail(e.getManagerEmail())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private CostCenter findEntityById(Long id) {
        return costCenterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cost center #" + id + " not found."));
    }

    private BusinessUnit resolveBu(Long buId) {
        if (buId == null) throw new IllegalArgumentException("Business Unit ID is required.");
        return businessUnitRepository.findById(buId)
                .orElseThrow(() -> new IllegalArgumentException("Business Unit #" + buId + " not found."));
    }

    private CostCenter.CostCenterType resolveType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) return null;
        try {
            return CostCenter.CostCenterType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cost center type: '" + typeStr + "'.");
        }
    }
}
