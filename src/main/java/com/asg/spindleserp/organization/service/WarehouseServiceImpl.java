package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.dto.WarehouseDTO;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.Warehouse;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.WarehouseRepository;
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
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository    warehouseRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public WarehouseDTO create(WarehouseDTO dto) {
        String code = dto.getWarehouseCode().trim().toUpperCase();
        if (warehouseRepository.existsByWarehouseCode(code)) {
            throw new IllegalArgumentException("Warehouse code '" + code + "' already exists.");
        }
        BusinessUnit bu = resolveBu(dto.getBusinessUnitId());

        Warehouse entity = Warehouse.builder()
                .businessUnit(bu)
                .warehouseCode(code)
                .warehouseName(dto.getWarehouseName().trim())
                .itemType(resolveItemType(dto.getItemType()))
                .address(dto.getAddress())
                .managerName(dto.getManagerName())
                .contactNumber(dto.getContactNumber())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return toDTO(warehouseRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public WarehouseDTO update(Long id, WarehouseDTO dto) {
        Warehouse entity = findEntityById(id);
        BusinessUnit bu = resolveBu(dto.getBusinessUnitId());
        String code = dto.getWarehouseCode().trim().toUpperCase();

        boolean codeChanged = !entity.getWarehouseCode().equalsIgnoreCase(code);
        if (codeChanged && warehouseRepository.existsByWarehouseCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("Warehouse code '" + code + "' already exists.");
        }

        entity.setBusinessUnit(bu);
        entity.setWarehouseCode(code);
        entity.setWarehouseName(dto.getWarehouseName().trim());
        entity.setItemType(resolveItemType(dto.getItemType()));
        entity.setAddress(dto.getAddress());
        entity.setManagerName(dto.getManagerName());
        entity.setContactNumber(dto.getContactNumber());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        return toDTO(warehouseRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public WarehouseDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDTO> findAll() {
        return warehouseRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDTO> findActiveByOrg(Long orgId) {
        return warehouseRepository.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDTO> findActiveByBusinessUnit(Long buId) {
        return warehouseRepository.findByBusinessUnitIdAndIsActiveTrue(buId)
                .stream().map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        warehouseRepository.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public WarehouseDTO toggleStatus(Long id) {
        Warehouse entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(warehouseRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE  (server-side, JdbcTemplate — mirrors BusinessUnitService)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {

        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "w.warehouse_code", "w.warehouse_name", "w.manager_name",
                        "bu.name", "w.item_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY w.id DESC)        AS sl,
                COUNT(*)     OVER ()                          AS full_count,
                w.id,
                w.warehouse_code                              AS code,
                w.warehouse_name                              AS name,
                w.item_type,
                bu.name                                       AS business_unit,
                COALESCE(w.manager_name, '—')                AS manager,
                COALESCE(w.contact_number, '—')              AS contact,
                TO_CHAR(w.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN w.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="whShow('   || w.id || ')" class="btn btn-white btn-sm" title="View">'
                    ||     '<i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="whEdit('   || w.id || ')" class="btn btn-white btn-sm" title="Edit">'
                    ||     '<i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="whToggle(' || w.id || ')" class="btn btn-white btn-sm" title="Toggle Status">'
                    ||     '<i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="whDelete(' || w.id || ')" class="btn btn-white btn-sm" title="Delete">'
                    ||     '<i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                               AS actions
            FROM  org_warehouses w
            JOIN  org_business_units bu ON bu.id = w.business_unit_id
            %s
            ORDER BY w.id DESC
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
    public WarehouseDTO toDTO(Warehouse e) {
        return WarehouseDTO.builder()
                .id(e.getId())
                .businessUnitId(e.getBusinessUnit() != null ? e.getBusinessUnit().getId()   : null)
                .businessUnitName(e.getBusinessUnit() != null ? e.getBusinessUnit().getName() : null)
                .organizationName(e.getBusinessUnit() != null && e.getBusinessUnit().getOrganization() != null
                        ? e.getBusinessUnit().getOrganization().getName() : null)
                .warehouseCode(e.getWarehouseCode())
                .warehouseName(e.getWarehouseName())
                .itemType(e.getItemType() != null ? e.getItemType().name() : null)
                .address(e.getAddress())
                .managerName(e.getManagerName())
                .contactNumber(e.getContactNumber())
                .active(e.isActive())
                .createdAt(e.getCreatedAt()  != null ? e.getCreatedAt().toString()  : null)
                .updatedAt(e.getUpdatedAt()  != null ? e.getUpdatedAt().toString()  : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Warehouse findEntityById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse #" + id + " not found."));
    }

    private BusinessUnit resolveBu(Long buId) {
        if (buId == null) throw new IllegalArgumentException("Business Unit ID is required.");
        return businessUnitRepository.findById(buId)
                .orElseThrow(() -> new IllegalArgumentException("Business Unit #" + buId + " not found."));
    }

    private ItemType resolveItemType(String typeStr) {
        if (typeStr == null || typeStr.isBlank())
            throw new IllegalArgumentException("Item Type is required.");
        try {
            return ItemType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Item Type: '" + typeStr + "'.");
        }
    }
}
