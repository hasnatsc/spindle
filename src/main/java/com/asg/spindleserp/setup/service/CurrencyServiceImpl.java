package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.setup.dto.CurrencyDTO;
import com.asg.spindleserp.setup.entity.Currency;
import com.asg.spindleserp.setup.repository.CurrencyRepository;
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
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final JdbcTemplate       jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public CurrencyDTO create(CurrencyDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (currencyRepository.existsByCode(code))
            throw new IllegalArgumentException("Currency code '" + code + "' already exists.");

        Currency entity = Currency.builder()
                .code(code)
                .name(dto.getName().trim())
                .symbol(dto.getSymbol())
                .decimalPlaces(dto.getDecimalPlaces() != null ? dto.getDecimalPlaces() : 2)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return toDTO(currencyRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public CurrencyDTO update(Long id, CurrencyDTO dto) {
        Currency entity = findEntityById(id);
        String code = dto.getCode().trim().toUpperCase();

        if (!entity.getCode().equalsIgnoreCase(code) && currencyRepository.existsByCodeAndIdNot(code, id))
            throw new IllegalArgumentException("Currency code '" + code + "' already exists.");

        entity.setCode(code);
        entity.setName(dto.getName().trim());
        entity.setSymbol(dto.getSymbol());
        entity.setDecimalPlaces(dto.getDecimalPlaces() != null ? dto.getDecimalPlaces() : entity.getDecimalPlaces());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(currencyRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CurrencyDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyDTO> findAll() {
        return currencyRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyDTO> findActive() {
        return currencyRepository.findByActiveTrue().stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { currencyRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public CurrencyDTO toggleStatus(Long id) {
        Currency entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(currencyRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList("c.code", "c.name", "c.symbol"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                c.id,
                c.code,
                c.name,
                COALESCE(c.symbol, '—')                     AS symbol,
                c.decimal_places,
                CASE WHEN c.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="currShow('   || c.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="currEdit('   || c.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="currToggle(' || c.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="currDelete(' || c.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM stp_currencies c
            %s
            ORDER BY c.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public CurrencyDTO toDTO(Currency e) {
        return CurrencyDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .symbol(e.getSymbol())
                .decimalPlaces(e.getDecimalPlaces())
                .active(e.isActive())
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private Currency findEntityById(Long id) {
        return currencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Currency #" + id + " not found."));
    }
}
