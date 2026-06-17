package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.dto.BusinessUnitDTO;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
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
public class BusinessUnitService {

    private final BusinessUnitRepository businessUnitRepository;
    private final OrganizationRepository organizationRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    public BusinessUnitDTO create(BusinessUnitDTO dto) {
        Organization org = resolveOrg(dto.getOrganizationId());

        if (businessUnitRepository.existsByOrganizationIdAndCode(org.getId(), dto.getCode())) {
            throw new IllegalArgumentException("Business unit code '" + dto.getCode() + "' already exists in this organisation.");
        }

        BusinessUnit entity = BusinessUnit.builder()
                .organization(org)
                .code(dto.getCode().trim().toUpperCase())
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return toDTO(businessUnitRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    public BusinessUnitDTO update(Long id, BusinessUnitDTO dto) {
        BusinessUnit entity = findEntityById(id);
        Organization org    = resolveOrg(dto.getOrganizationId());

        boolean codeChanged = !entity.getCode().equalsIgnoreCase(dto.getCode());
        if (codeChanged && businessUnitRepository.existsByOrganizationIdAndCode(org.getId(), dto.getCode())) {
            throw new IllegalArgumentException("Business unit code '" + dto.getCode() + "' already exists in this organisation.");
        }

        entity.setOrganization(org);
        entity.setCode(dto.getCode().trim().toUpperCase());
        entity.setName(dto.getName().trim());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        return toDTO(businessUnitRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BusinessUnitDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitDTO> findAll() {
        return businessUnitRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitDTO> findActiveByOrg(Long orgId) {
        return businessUnitRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        businessUnitRepository.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    public BusinessUnitDTO toggleStatus(Long id) {
        BusinessUnit entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(businessUnitRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 " + CommonUtils.searchILike(search, Arrays.asList("bu.code", "bu.name", "bu.description", "o.name", "o.code"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY bu.id DESC)      AS sl,
                COUNT(*)     OVER ()                          AS full_count,
                bu.id,
                bu.code,
                bu.name,
                bu.description,
                o.name                                        AS organization_name,
                TO_CHAR(bu.created_at, 'DD-Mon-YYYY')        AS created_at,
                CASE WHEN bu.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="buShow('  || bu.id || ')" class="btn btn-white btn-sm" title="View">'
                    ||     '<i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="buEdit('  || bu.id || ')" class="btn btn-white btn-sm" title="Edit">'
                    ||     '<i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="buToggle(' || bu.id || ')" class="btn btn-white btn-sm" title="Toggle Status">'
                    ||     '<i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="buDelete(' || bu.id || ')" class="btn btn-white btn-sm" title="Delete">'
                    ||     '<i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                               AS actions
            FROM  org_business_units bu
            LEFT  JOIN org_organizations o ON o.id = bu.organization_id
            %s
            ORDER BY bu.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));

        return DataTableResponse.of(draw, total, total, rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private BusinessUnit findEntityById(Long id) {
        return businessUnitRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Business unit #" + id + " not found."));
    }

    private Organization resolveOrg(Long orgId) {
        if (orgId == null) throw new IllegalArgumentException("Organisation ID is required.");
        return organizationRepository.findById(orgId).orElseThrow(() -> new IllegalArgumentException("Organisation #" + orgId + " not found."));
    }

    public BusinessUnitDTO toDTO(BusinessUnit e) {
        return BusinessUnitDTO.builder()
                .id(e.getId())
                .organizationId(e.getOrganization() != null ? e.getOrganization().getId()   : null)
                .organizationName(e.getOrganization() != null ? e.getOrganization().getName() : null)
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedBy(e.getUpdatedBy())
                .active(e.isActive())
                .build();
    }
}
