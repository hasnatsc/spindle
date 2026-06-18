package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.setup.dto.DocumentSequenceDTO;
import com.asg.spindleserp.setup.entity.DocumentSequence;
import com.asg.spindleserp.setup.repository.DocumentSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentSequenceServiceImpl implements DocumentSequenceService {

    private final DocumentSequenceRepository sequenceRepository;
    private final JdbcTemplate               jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public DocumentSequenceDTO create(DocumentSequenceDTO dto) {
        Long   orgId    = dto.getOrganizationId();
        String prefix   = dto.getPrefix().trim().toUpperCase();
        String yearCode = dto.getYearCode().trim();

        if (sequenceRepository.existsByOrganizationIdAndPrefixAndYearCode(orgId, prefix, yearCode))
            throw new IllegalArgumentException(
                    "Sequence for prefix '" + prefix + "' / year '" + yearCode + "' already exists.");

        DocumentSequence entity = DocumentSequence.builder()
                .organizationId(orgId)
                .prefix(prefix)
                .yearCode(yearCode)
                .lastSeq(dto.getLastSeq() != null ? dto.getLastSeq() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toDTO(sequenceRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public DocumentSequenceDTO update(Long id, DocumentSequenceDTO dto) {
        DocumentSequence entity = findEntityById(id);
        Long   orgId    = dto.getOrganizationId();
        String prefix   = dto.getPrefix().trim().toUpperCase();
        String yearCode = dto.getYearCode().trim();

        boolean changed = !entity.getPrefix().equals(prefix) || !entity.getYearCode().equals(yearCode);
        if (changed && sequenceRepository.existsByOrganizationIdAndPrefixAndYearCodeAndIdNot(orgId, prefix, yearCode, id))
            throw new IllegalArgumentException(
                    "Sequence for prefix '" + prefix + "' / year '" + yearCode + "' already exists.");

        entity.setOrganizationId(orgId);
        entity.setPrefix(prefix);
        entity.setYearCode(yearCode);
        // Allow admin reset — only decrease if explicitly sent; never null
        if (dto.getLastSeq() != null) entity.setLastSeq(dto.getLastSeq());
        entity.setUpdatedAt(LocalDateTime.now());
        return toDTO(sequenceRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DocumentSequenceDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<DocumentSequenceDTO> findAll() {
        return sequenceRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<DocumentSequenceDTO> findByOrg(Long orgId) {
        return sequenceRepository.findByOrganizationIdOrderByPrefixAscYearCodeDesc(orgId)
                .stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { sequenceRepository.delete(findEntityById(id)); }

    // ── NEXT DOCUMENT NUMBER ──────────────────────────────────────────────────

    /**
     * Atomically increments the counter and returns the formatted document number.
     * Format: {PREFIX}-{YY}-{NNNNNN}
     * If no row exists, one is created automatically (seed = 1).
     */
    @Override
    public String nextDocumentNumber(Long orgId, String prefix, String yearCode) {
        String p = prefix.trim().toUpperCase();
        String y = yearCode.trim();

        // Upsert — create seed row if missing
        if (!sequenceRepository.existsByOrganizationIdAndPrefixAndYearCode(orgId, p, y)) {
            DocumentSequence seed = DocumentSequence.builder()
                    .organizationId(orgId)
                    .prefix(p).yearCode(y).lastSeq(0)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            sequenceRepository.saveAndFlush(seed);
        }

        sequenceRepository.increment(orgId, p, y);
        sequenceRepository.flush();

        DocumentSequence seq = sequenceRepository
                .findByOrganizationIdAndPrefixAndYearCode(orgId, p, y)
                .orElseThrow(() -> new IllegalStateException("Sequence row missing after increment."));

        return String.format("%s-%s-%06d", p, y, seq.getLastSeq());
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList("ds.prefix", "ds.year_code"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY ds.id DESC)      AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                ds.id,
                ds.prefix,
                ds.year_code,
                ds.last_seq,
                ds.prefix || '-' || ds.year_code || '-' || LPAD(ds.last_seq::text, 6, '0') AS last_number,
                TO_CHAR(ds.updated_at, 'DD-Mon-YYYY HH24:MI')                               AS updated_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="seqShow('   || ds.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="seqEdit('   || ds.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="seqDelete(' || ds.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM stp_document_sequences ds
            %s
            ORDER BY ds.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public DocumentSequenceDTO toDTO(DocumentSequence e) {
        return DocumentSequenceDTO.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .prefix(e.getPrefix())
                .yearCode(e.getYearCode())
                .lastSeq(e.getLastSeq())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private DocumentSequence findEntityById(Long id) {
        return sequenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document Sequence #" + id + " not found."));
    }
}
