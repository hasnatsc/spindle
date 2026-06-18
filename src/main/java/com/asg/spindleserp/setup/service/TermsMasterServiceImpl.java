package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.setup.dto.TermsMasterDTO;
import com.asg.spindleserp.setup.entity.TermsMaster;
import com.asg.spindleserp.setup.repository.TermsMasterRepository;
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
public class TermsMasterServiceImpl implements TermsMasterService {

    private final TermsMasterRepository termsRepository;
    private final JdbcTemplate          jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public TermsMasterDTO create(TermsMasterDTO dto) {
        String title   = dto.getTitle().trim();
        String docType = dto.getDocumentType().trim().toUpperCase();
        if (termsRepository.existsByTitleAndDocumentType(title, docType))
            throw new IllegalArgumentException("Terms '" + title + "' already exists for document type '" + docType + "'.");

        TermsMaster entity = TermsMaster.builder()
                .title(title)
                .description(dto.getDescription())
                .documentType(docType)
                .sortOrder(dto.getSortOrder())
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .isDefault(Boolean.TRUE.equals(dto.getIsDefault()))
                .build();
        return toDTO(termsRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public TermsMasterDTO update(Long id, TermsMasterDTO dto) {
        TermsMaster entity = findEntityById(id);
        String title   = dto.getTitle().trim();
        String docType = dto.getDocumentType().trim().toUpperCase();

        if ((!entity.getTitle().equals(title) || !entity.getDocumentType().equals(docType))
                && termsRepository.existsByTitleAndDocumentTypeAndIdNot(title, docType, id))
            throw new IllegalArgumentException("Terms '" + title + "' already exists for document type '" + docType + "'.");

        entity.setTitle(title);
        entity.setDescription(dto.getDescription());
        entity.setDocumentType(docType);
        entity.setSortOrder(dto.getSortOrder());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        entity.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        return toDTO(termsRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public TermsMasterDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<TermsMasterDTO> findAll() {
        return termsRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TermsMasterDTO> findActiveByDocumentType(String documentType) {
        return termsRepository.findByDocumentTypeAndIsActiveTrue(documentType.trim().toUpperCase())
                .stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { termsRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public TermsMasterDTO toggleStatus(Long id) {
        TermsMaster entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(termsRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList("t.title", "t.document_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY t.sort_order ASC NULLS LAST, t.id DESC) AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                t.id,
                t.title,
                t.document_type,
                COALESCE(t.sort_order::text, '—')           AS sort_order,
                CASE WHEN t.is_default THEN '<span class="badge bg-warning text-dark">Default</span>' ELSE '' END AS default_flag,
                CASE WHEN t.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="termsShow('   || t.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="termsEdit('   || t.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="termsToggle(' || t.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="termsDelete(' || t.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM stp_terms_master t
            %s
            ORDER BY t.sort_order ASC NULLS LAST, t.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public TermsMasterDTO toDTO(TermsMaster e) {
        return TermsMasterDTO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .documentType(e.getDocumentType())
                .sortOrder(e.getSortOrder())
                .active(e.isActive())
                .isDefault(e.isDefault())
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private TermsMaster findEntityById(Long id) {
        return termsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terms #" + id + " not found."));
    }
}
