package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.dto.DepartmentDTO;
import com.asg.spindleserp.organization.entity.Department;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.DepartmentRepository;
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
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository   departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public DepartmentDTO create(DepartmentDTO dto) {
        Organization org = resolveOrg(dto.getOrganizationId());
        String name = dto.getName().trim();
        String code = dto.getCode() != null ? dto.getCode().trim().toUpperCase() : null;

        if (departmentRepository.existsByName(name)) {
            throw new IllegalArgumentException("Department name '" + name + "' already exists.");
        }
        if (code != null && departmentRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Department code '" + code + "' already exists.");
        }

        Department entity = Department.builder()
                .organization(org)
                .code(code)
                .name(name)
                .description(dto.getDescription())
                .headEmployeeId(dto.getHeadEmployeeId())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        if (dto.getParentDepartmentId() != null) {
            entity.setParentDepartment(findEntityById(dto.getParentDepartmentId()));
        }

        return toDTO(departmentRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public DepartmentDTO update(Long id, DepartmentDTO dto) {
        Department entity = findEntityById(id);
        Organization org  = resolveOrg(dto.getOrganizationId());
        String name = dto.getName().trim();
        String code = dto.getCode() != null ? dto.getCode().trim().toUpperCase() : null;

        if (!entity.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameAndIdNot(name, id)) {
            throw new IllegalArgumentException("Department name '" + name + "' already exists.");
        }
        if (code != null && !code.equalsIgnoreCase(entity.getCode())
                && departmentRepository.existsByCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("Department code '" + code + "' already exists.");
        }

        entity.setOrganization(org);
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(dto.getDescription());
        entity.setHeadEmployeeId(dto.getHeadEmployeeId());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        // Parent — guard against self-reference
        if (dto.getParentDepartmentId() != null && !dto.getParentDepartmentId().equals(id)) {
            entity.setParentDepartment(findEntityById(dto.getParentDepartmentId()));
        } else {
            entity.setParentDepartment(null);
        }

        return toDTO(departmentRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findAll() {
        return departmentRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findActiveByOrg(Long orgId) {
        return departmentRepository.findByOrganizationIdAndActiveTrue(orgId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findParentCandidates(Long excludeId) {
        return departmentRepository.findAll().stream()
                .filter(d -> d.isActive() && !d.getId().equals(excludeId))
                .map(this::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        if (departmentRepository.existsByParentDepartmentId(id)) {
            throw new IllegalArgumentException("Cannot delete: child departments exist.");
        }
        departmentRepository.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public DepartmentDTO toggleStatus(Long id) {
        Department entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(departmentRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE  (server-side, JdbcTemplate — mirrors BusinessUnitService)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {

        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "d.code", "d.name", "d.description", "o.name", "o.code"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY d.id DESC)        AS sl,
                COUNT(*)     OVER ()                          AS full_count,
                d.id,
                COALESCE(d.code, '—')                        AS code,
                d.name,
                o.name                                        AS organization,
                COALESCE(p.name, '—')                        AS parent,
                COALESCE(d.description, '—')                 AS description,
                TO_CHAR(d.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN d.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="deptShow('   || d.id || ')" class="btn btn-white btn-sm" title="View">'
                    ||     '<i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="deptEdit('   || d.id || ')" class="btn btn-white btn-sm" title="Edit">'
                    ||     '<i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="deptToggle(' || d.id || ')" class="btn btn-white btn-sm" title="Toggle Status">'
                    ||     '<i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="deptDelete(' || d.id || ')" class="btn btn-white btn-sm" title="Delete">'
                    ||     '<i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                               AS actions
            FROM  org_departments d
            JOIN  org_organizations o    ON o.id = d.organization_id
            LEFT  JOIN org_departments p ON p.id = d.parent_department_id
            %s
            ORDER BY d.id DESC
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
    public DepartmentDTO toDTO(Department e) {
        return DepartmentDTO.builder()
                .id(e.getId())
                .organizationId(e.getOrganization()   != null ? e.getOrganization().getId()   : null)
                .organizationName(e.getOrganization() != null ? e.getOrganization().getName() : null)
                .parentDepartmentId(e.getParentDepartment()   != null ? e.getParentDepartment().getId()   : null)
                .parentDepartmentName(e.getParentDepartment() != null ? e.getParentDepartment().getName() : null)
                .headEmployeeId(e.getHeadEmployeeId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
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

    private Department findEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department #" + id + " not found."));
    }

    private Organization resolveOrg(Long orgId) {
        if (orgId == null) throw new IllegalArgumentException("Organisation ID is required.");
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation #" + orgId + " not found."));
    }
}
