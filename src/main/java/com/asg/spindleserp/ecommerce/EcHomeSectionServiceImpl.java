// Path: com/asg/spindleserp/ecommerce/service/EcHomeSectionServiceImpl.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.cms.EcHomeSection;
import com.asg.spindleserp.ecommerce.cms.EcHomeSectionProduct;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductCatalogRepository;
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
public class EcHomeSectionServiceImpl implements EcHomeSectionService {

    private final EcHomeSectionRepository     sectionRepository;
    private final EcProductCatalogRepository catalogRepository;
    private final JdbcTemplate                jdbcTemplate;

    @Override
    public EcHomeSectionDTO create(EcHomeSectionDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = dto.getSectionCode().trim().toUpperCase();
        if (sectionRepository.existsByOrganizationIdAndSectionCode(orgId, code))
            throw new IllegalArgumentException("Section code '" + code + "' already exists.");

        EcHomeSection entity = EcHomeSection.builder()
                .sectionCode(code)
                .sectionName(dto.getSectionName().trim())
                .sectionTitle(dto.getSectionTitle())
                .sectionSubtitle(dto.getSectionSubtitle())
                .sectionType(parseSectionType(dto.getSectionType()))
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .maxProducts(dto.getMaxProducts() != null ? dto.getMaxProducts() : 12)
                .active(dto.getActive() == null || dto.getActive())
                .build();

        entity = sectionRepository.save(entity);
        syncSectionProducts(entity, dto.getSectionProducts());
        return toDTO(sectionRepository.save(entity));
    }

    @Override
    public EcHomeSectionDTO update(Long id, EcHomeSectionDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcHomeSection entity = findEntityById(id);
        String code = dto.getSectionCode().trim().toUpperCase();
        if (!entity.getSectionCode().equals(code) &&
                sectionRepository.existsByOrganizationIdAndSectionCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Section code '" + code + "' already exists.");

        entity.setSectionCode(code);
        entity.setSectionName(dto.getSectionName().trim());
        entity.setSectionTitle(dto.getSectionTitle());
        entity.setSectionSubtitle(dto.getSectionSubtitle());
        entity.setSectionType(parseSectionType(dto.getSectionType()));
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : entity.getDisplayOrder());
        entity.setMaxProducts(dto.getMaxProducts() != null ? dto.getMaxProducts() : entity.getMaxProducts());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        syncSectionProducts(entity, dto.getSectionProducts());
        return toDTO(sectionRepository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcHomeSectionDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcHomeSectionDTO> findAllByOrg(Long orgId) {
        return sectionRepository.findByOrganizationIdOrderByDisplayOrder(orgId).stream().map(this::toDTO).toList();
    }

    @Override
    public void delete(Long id) { sectionRepository.delete(findEntityById(id)); }

    @Override
    public EcHomeSectionDTO toggleStatus(Long id) {
        EcHomeSection e = findEntityById(id); e.setActive(!e.isActive());
        return toDTO(sectionRepository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE s.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList("s.section_code", "s.section_name", "s.section_type"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY s.display_order, s.id) AS sl,
                   COUNT(*) OVER () AS full_count,
                   s.id, s.section_code, s.section_name, s.section_type, s.display_order, s.max_products,
                   (SELECT COUNT(*) FROM ec_home_section_products sp WHERE sp.section_id = s.id) AS product_count,
                   TO_CHAR(s.created_at, 'DD-Mon-YYYY') AS created_at,
                   CASE WHEN s.active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-danger">Inactive</span>' END AS status,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="echsShow('   || s.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="echsEdit('   || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="echsToggle(' || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="echsDelete(' || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM ec_home_sections s %s ORDER BY s.display_order, s.id OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcHomeSectionDTO toDTO(EcHomeSection e) {
        List<EcHomeSectionDTO.SectionProductDTO> products = e.getSectionProducts().stream().map(sp ->
            EcHomeSectionDTO.SectionProductDTO.builder()
                .id(sp.getId())
                .productId(sp.getProduct() != null ? sp.getProduct().getId() : null)
                .productTitle(sp.getProduct() != null ? sp.getProduct().getProductTitle() : null)
                .productSlug(sp.getProduct() != null ? sp.getProduct().getSlug() : null)
                .displayOrder(sp.getDisplayOrder())
                .build()
        ).toList();

        return EcHomeSectionDTO.builder()
                .id(e.getId()).sectionCode(e.getSectionCode()).sectionName(e.getSectionName())
                .sectionTitle(e.getSectionTitle()).sectionSubtitle(e.getSectionSubtitle())
                .sectionType(e.getSectionType() != null ? e.getSectionType().name() : null)
                .displayOrder(e.getDisplayOrder()).maxProducts(e.getMaxProducts()).active(e.isActive())
                .sectionProducts(products)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).build();
    }

    // ── Sync section products (clear-and-reinsert) ─────────────────────────
    private void syncSectionProducts(EcHomeSection entity, List<EcHomeSectionDTO.SectionProductDTO> dtoList) {
        entity.getSectionProducts().clear();
        if (dtoList == null || dtoList.isEmpty()) return;
        dtoList.forEach(d -> {
            if (d.getProductId() == null) return;
            EcProductCatalog prod = catalogRepository.findById(d.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product #" + d.getProductId() + " not found."));
            entity.getSectionProducts().add(EcHomeSectionProduct.builder()
                    .section(entity).product(prod)
                    .displayOrder(d.getDisplayOrder() != null ? d.getDisplayOrder() : 0)
                    .build());
        });
    }

    private EcHomeSection findEntityById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Home section #" + id + " not found."));
    }

    private EcHomeSection.SectionType parseSectionType(String v) {
        if (v == null || v.isBlank()) return EcHomeSection.SectionType.CUSTOM;
        try { return EcHomeSection.SectionType.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return EcHomeSection.SectionType.CUSTOM; }
    }
}
