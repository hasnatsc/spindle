// Path: com/asg/spindleserp/ecommerce/service/EcTaxClassServiceImpl.java
package com.asg.spindleserp.ecommerce.settings.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.settings.dto.EcTaxClassDTO;
import com.asg.spindleserp.ecommerce.settings.entity.EcTaxClass;
import com.asg.spindleserp.ecommerce.settings.repository.EcTaxClassRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j @Service @Transactional @RequiredArgsConstructor
public class EcTaxClassServiceImpl implements EcTaxClassService {

    private final EcTaxClassRepository taxClassRepository;
    private final JdbcTemplate         jdbcTemplate;

    @Override
    public EcTaxClassDTO create(EcTaxClassDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = dto.getClassCode().trim().toUpperCase();
        if (taxClassRepository.existsByOrganizationIdAndClassCode(orgId, code))
            throw new IllegalArgumentException("Tax class code '" + code + "' already exists.");
        EcTaxClass entity = EcTaxClass.builder()
                .classCode(code).className(dto.getClassName().trim())
                .description(dto.getDescription())
                .active(dto.getActive() == null || dto.getActive())
                .build();
        entity = taxClassRepository.save(entity);
        // NOTE: EcTaxRule does NOT have OneToMany on EcTaxClass (only ManyToOne the other way).
        // Rules are managed separately via JDBC inserts here for efficiency.
        syncRulesViaJdbc(entity.getId(), dto.getRules());
        return toDTO(taxClassRepository.findById(entity.getId()).orElse(entity));
    }

    @Override
    public EcTaxClassDTO update(Long id, EcTaxClassDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcTaxClass entity = findEntityById(id);
        String code = dto.getClassCode().trim().toUpperCase();
        if (!entity.getClassCode().equals(code) &&
                taxClassRepository.existsByOrganizationIdAndClassCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Tax class code '" + code + "' already exists.");
        entity.setClassCode(code); entity.setClassName(dto.getClassName().trim());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        taxClassRepository.save(entity);
        syncRulesViaJdbc(id, dto.getRules());
        return toDTO(taxClassRepository.findById(id).orElse(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcTaxClassDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcTaxClassDTO> findActiveByOrg(Long orgId) {
        return taxClassRepository.findByOrganizationIdAndActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM ec_tax_rules WHERE tax_class_id = ?", id);
        taxClassRepository.delete(findEntityById(id));
    }

    @Override
    public EcTaxClassDTO toggleStatus(Long id) {
        EcTaxClass e = findEntityById(id); e.setActive(!e.isActive());
        return toDTO(taxClassRepository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE tc.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList("tc.class_code", "tc.class_name"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY tc.id DESC) AS sl, COUNT(*) OVER () AS full_count,
                   tc.id, tc.class_code, tc.class_name,
                   (SELECT COUNT(*) FROM ec_tax_rules tr WHERE tr.tax_class_id = tc.id AND tr.active = true) AS rule_count,
                   TO_CHAR(tc.created_at, 'DD-Mon-YYYY') AS created_at,
                   CASE WHEN tc.active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-danger">Inactive</span>' END AS status,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="ectaxShow('   || tc.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="ectaxEdit('   || tc.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="ectaxToggle(' || tc.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="ectaxDelete(' || tc.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM ec_tax_classes tc %s ORDER BY tc.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcTaxClassDTO toDTO(EcTaxClass e) {
        // Load rules via JDBC to avoid needing a OneToMany on EcTaxClass entity
        List<EcTaxClassDTO.RuleDTO> rules = jdbcTemplate.queryForList(
                "SELECT * FROM ec_tax_rules WHERE tax_class_id = ? ORDER BY id", e.getId())
            .stream().map(r -> EcTaxClassDTO.RuleDTO.builder()
                .id(CommonUtils.toLong(r.get("id")))
                .country((String) r.get("country"))
                .division((String) r.get("division"))
                .district((String) r.get("district"))
                .taxName((String) r.get("tax_name"))
                .taxPercent(r.get("tax_percent") != null ? new java.math.BigDecimal(r.get("tax_percent").toString()) : null)
                .effectiveFrom(r.get("effective_from") != null ? ((java.sql.Date) r.get("effective_from")).toLocalDate() : null)
                .effectiveTo(r.get("effective_to") != null ? ((java.sql.Date) r.get("effective_to")).toLocalDate() : null)
                .active((Boolean) r.get("active"))
                .build()
            ).toList();

        return EcTaxClassDTO.builder()
                .id(e.getId()).classCode(e.getClassCode()).className(e.getClassName())
                .description(e.getDescription()).active(e.isActive()).rules(rules)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).build();
    }

    // ── Rules via JDBC (EcTaxRule has no OneToMany back to EcTaxClass) ────────
    private void syncRulesViaJdbc(Long taxClassId, List<EcTaxClassDTO.RuleDTO> rules) {
        jdbcTemplate.update("DELETE FROM ec_tax_rules WHERE tax_class_id = ?", taxClassId);
        Long orgId = ContextProvider.getOrganizationId();
        if (rules == null || rules.isEmpty()) return;
        rules.forEach(r -> jdbcTemplate.update(
            "INSERT INTO ec_tax_rules (organization_id, tax_class_id, country, division, district, tax_name, tax_percent, effective_from, effective_to, active) VALUES (?,?,?,?,?,?,?,?,?,?)",
            orgId, taxClassId,
            r.getCountry() != null ? r.getCountry() : "Bangladesh",
            r.getDivision(), r.getDistrict(), r.getTaxName(), r.getTaxPercent(),
            r.getEffectiveFrom(), r.getEffectiveTo(),
            r.getActive() == null || r.getActive()
        ));
    }

    private EcTaxClass findEntityById(Long id) {
        return taxClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tax class #" + id + " not found."));
    }
}
