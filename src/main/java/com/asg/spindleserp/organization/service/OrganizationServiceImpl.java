package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.dto.OrganizationDTO;
import com.asg.spindleserp.organization.entity.Organization;
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
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final JdbcTemplate           jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public OrganizationDTO create(OrganizationDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (organizationRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Organisation code '" + code + "' already exists.");
        }
        Organization entity = Organization.builder()
                .code(code)
                .name(dto.getName().trim())
                .nameBn(dto.getNameBn())
                .about(dto.getAbout())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .website(dto.getWebsite())
                .taxId(dto.getTaxId())
                .vatNo(dto.getVatNo())
                .binNo(dto.getBinNo())
                .logoUrl(dto.getLogoUrl())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return toDTO(organizationRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public OrganizationDTO update(Long id, OrganizationDTO dto) {
        Organization entity = findEntityById(id);
        String code = dto.getCode().trim().toUpperCase();

        boolean codeChanged = !entity.getCode().equalsIgnoreCase(code);
        if (codeChanged && organizationRepository.existsByCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("Organisation code '" + code + "' already exists.");
        }

        entity.setCode(code);
        entity.setName(dto.getName().trim());
        entity.setNameBn(dto.getNameBn());
        entity.setAbout(dto.getAbout());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry());
        entity.setPostalCode(dto.getPostalCode());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setWebsite(dto.getWebsite());
        entity.setTaxId(dto.getTaxId());
        entity.setVatNo(dto.getVatNo());
        entity.setBinNo(dto.getBinNo());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());

        return toDTO(organizationRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrganizationDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationDTO> findAll() {
        return organizationRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationDTO> findActive() {
        return organizationRepository.findByIsActiveTrue().stream().map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        organizationRepository.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public OrganizationDTO toggleStatus(Long id) {
        Organization entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(organizationRepository.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE  (server-side, JdbcTemplate — mirrors BusinessUnitService)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {

        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "o.code", "o.name", "o.name_bn", "o.email", "o.phone", "o.country"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY o.id DESC)       AS sl,
                COUNT(*)     OVER ()                          AS full_count,
                o.id,
                o.code,
                o.name,
                COALESCE(o.name_bn, '—')                     AS name_bn,
                COALESCE(o.email,   '—')                     AS email,
                COALESCE(o.phone,   '—')                     AS phone,
                COALESCE(o.country, '—')                     AS country,
                TO_CHAR(o.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN o.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="orgShow('   || o.id || ')" class="btn btn-white btn-sm" title="View">'
                    ||     '<i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="orgEdit('   || o.id || ')" class="btn btn-white btn-sm" title="Edit">'
                    ||     '<i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="orgToggle(' || o.id || ')" class="btn btn-white btn-sm" title="Toggle Status">'
                    ||     '<i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="orgDelete(' || o.id || ')" class="btn btn-white btn-sm" title="Delete">'
                    ||     '<i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                               AS actions
            FROM  org_organizations o
            %s
            ORDER BY o.id DESC
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
    public OrganizationDTO toDTO(Organization e) {
        return OrganizationDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .nameBn(e.getNameBn())
                .about(e.getAbout())
                .address(e.getAddress())
                .city(e.getCity())
                .state(e.getState())
                .country(e.getCountry())
                .postalCode(e.getPostalCode())
                .phone(e.getPhone())
                .email(e.getEmail())
                .website(e.getWebsite())
                .taxId(e.getTaxId())
                .vatNo(e.getVatNo())
                .binNo(e.getBinNo())
                .logoUrl(e.getLogoUrl())
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

    private Organization findEntityById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organisation #" + id + " not found."));
    }
}
