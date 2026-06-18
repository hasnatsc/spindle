package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.setup.dto.CountryDTO;
import com.asg.spindleserp.setup.entity.Country;
import com.asg.spindleserp.setup.entity.Currency;
import com.asg.spindleserp.setup.repository.CountryRepository;
import com.asg.spindleserp.setup.repository.CurrencyRepository;
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
public class CountryServiceImpl implements CountryService {

    private final CountryRepository  countryRepository;
    private final CurrencyRepository currencyRepository;
    private final JdbcTemplate       jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public CountryDTO create(CountryDTO dto) {
        String code = dto.getIsoCode().trim().toUpperCase();
        if (countryRepository.existsByIsoCode(code))
            throw new IllegalArgumentException("ISO Code '" + code + "' already exists.");

        Currency currency = resolveCurrency(dto.getCurrencyId());

        Country entity = Country.builder()
                .currency(currency)
                .isoCode(code)
                .isoCode2(dto.getIsoCode2().trim().toUpperCase())
                .name(dto.getName().trim())
                .nameNative(dto.getNameNative())
                .phoneCode(dto.getPhoneCode())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .createdAt(LocalDateTime.now())
                .build();
        return toDTO(countryRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public CountryDTO update(Long id, CountryDTO dto) {
        Country entity   = findEntityById(id);
        String code      = dto.getIsoCode().trim().toUpperCase();
        Currency currency = resolveCurrency(dto.getCurrencyId());

        if (!entity.getIsoCode().equalsIgnoreCase(code) && countryRepository.existsByIsoCodeAndIdNot(code, id))
            throw new IllegalArgumentException("ISO Code '" + code + "' already exists.");

        entity.setCurrency(currency);
        entity.setIsoCode(code);
        entity.setIsoCode2(dto.getIsoCode2().trim().toUpperCase());
        entity.setName(dto.getName().trim());
        entity.setNameNative(dto.getNameNative());
        entity.setPhoneCode(dto.getPhoneCode());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(countryRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public CountryDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<CountryDTO> findAll() {
        return countryRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<CountryDTO> findActive() {
        return countryRepository.findByActiveTrue().stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { countryRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public CountryDTO toggleStatus(Long id) {
        Country entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(countryRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "co.iso_code", "co.iso_code2", "co.name", "co.name_native",
                        "co.phone_code", "cu.code"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY co.id DESC)      AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                co.id,
                co.iso_code,
                co.iso_code2,
                co.name,
                COALESCE(co.name_native, '—')               AS name_native,
                COALESCE(co.phone_code,  '—')               AS phone_code,
                cu.code                                      AS currency_code,
                CASE WHEN co.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="countryShow('   || co.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="countryEdit('   || co.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="countryToggle(' || co.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="countryDelete(' || co.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM stp_location_countries co
            JOIN stp_currencies cu ON cu.id = co.currency_id
            %s
            ORDER BY co.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public CountryDTO toDTO(Country e) {
        return CountryDTO.builder()
                .id(e.getId())
                .currencyId(e.getCurrency()   != null ? e.getCurrency().getId()   : null)
                .currencyCode(e.getCurrency() != null ? e.getCurrency().getCode() : null)
                .currencyName(e.getCurrency() != null ? e.getCurrency().getName() : null)
                .isoCode(e.getIsoCode())
                .isoCode2(e.getIsoCode2())
                .name(e.getName())
                .nameNative(e.getNameNative())
                .phoneCode(e.getPhoneCode())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private Country findEntityById(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Country #" + id + " not found."));
    }

    private Currency resolveCurrency(Long currencyId) {
        if (currencyId == null) throw new IllegalArgumentException("Currency ID is required.");
        return currencyRepository.findById(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Currency #" + currencyId + " not found."));
    }
}
