package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.setup.dto.BankDTO;
import com.asg.spindleserp.setup.entity.Bank;
import com.asg.spindleserp.setup.repository.BankRepository;
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
public class BankServiceImpl implements BankService {

    private final BankRepository bankRepository;
    private final JdbcTemplate   jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public BankDTO create(BankDTO dto) {
        Long orgId = dto.getOrganizationId();
        String code = dto.getBankCode().trim().toUpperCase();
        if (bankRepository.existsByOrganizationIdAndBankCode(orgId, code))
            throw new IllegalArgumentException("Bank code '" + code + "' already exists in this organisation.");

        Bank entity = Bank.builder()
                .organizationId(orgId)
                .bankCode(code)
                .bankName(dto.getBankName().trim())
                .bankNameLocal(dto.getBankNameLocal())
                .shortName(dto.getShortName())
                .bankType(Bank.BankType.valueOf(dto.getBankType() != null ? dto.getBankType() : "COMMERCIAL"))
                .bankCategory(dto.getBankCategory())
                .swiftCode(dto.getSwiftCode())
                .centralBankCode(dto.getCentralBankCode())
                .routingNumberPrefix(dto.getRoutingNumberPrefix())
                .headOfficeAddress(dto.getHeadOfficeAddress())
                .headOfficeCity(dto.getHeadOfficeCity())
                .headOfficeCountry(dto.getHeadOfficeCountry())
                .headOfficePhone(dto.getHeadOfficePhone())
                .headOfficeEmail(dto.getHeadOfficeEmail())
                .website(dto.getWebsite())
                .correspondentBankName(dto.getCorrespondentBankName())
                .correspondentSwiftCode(dto.getCorrespondentSwiftCode())
                .correspondentAccountNumber(dto.getCorrespondentAccountNumber())
                .rating(Bank.BankRating.valueOf(dto.getRating() != null ? dto.getRating() : "UNRATED"))
                .supportsLc(Boolean.TRUE.equals(dto.getSupportsLc()))
                .supportsImportLc(Boolean.TRUE.equals(dto.getSupportsImportLc()))
                .supportsExportLc(Boolean.TRUE.equals(dto.getSupportsExportLc()))
                .supportsBtbLc(Boolean.TRUE.equals(dto.getSupportsBtbLc()))
                .supportsInlandLc(Boolean.TRUE.equals(dto.getSupportsInlandLc()))
                .supportsOnlineBanking(Boolean.TRUE.equals(dto.getSupportsOnlineBanking()))
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return toDTO(bankRepository.save(entity));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public BankDTO update(Long id, BankDTO dto) {
        Bank entity = findEntityById(id);
        Long orgId  = dto.getOrganizationId();
        String code = dto.getBankCode().trim().toUpperCase();

        if (!entity.getBankCode().equalsIgnoreCase(code)
                && bankRepository.existsByOrganizationIdAndBankCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Bank code '" + code + "' already exists in this organisation.");

        entity.setOrganizationId(orgId);
        entity.setBankCode(code);
        entity.setBankName(dto.getBankName().trim());
        entity.setBankNameLocal(dto.getBankNameLocal());
        entity.setShortName(dto.getShortName());
        entity.setBankType(dto.getBankType() != null ? Bank.BankType.valueOf(dto.getBankType()) : entity.getBankType());
        entity.setBankCategory(dto.getBankCategory());
        entity.setSwiftCode(dto.getSwiftCode());
        entity.setCentralBankCode(dto.getCentralBankCode());
        entity.setRoutingNumberPrefix(dto.getRoutingNumberPrefix());
        entity.setHeadOfficeAddress(dto.getHeadOfficeAddress());
        entity.setHeadOfficeCity(dto.getHeadOfficeCity());
        entity.setHeadOfficeCountry(dto.getHeadOfficeCountry());
        entity.setHeadOfficePhone(dto.getHeadOfficePhone());
        entity.setHeadOfficeEmail(dto.getHeadOfficeEmail());
        entity.setWebsite(dto.getWebsite());
        entity.setCorrespondentBankName(dto.getCorrespondentBankName());
        entity.setCorrespondentSwiftCode(dto.getCorrespondentSwiftCode());
        entity.setCorrespondentAccountNumber(dto.getCorrespondentAccountNumber());
        entity.setRating(dto.getRating() != null ? Bank.BankRating.valueOf(dto.getRating()) : entity.getRating());
        entity.setSupportsLc(Boolean.TRUE.equals(dto.getSupportsLc()));
        entity.setSupportsImportLc(Boolean.TRUE.equals(dto.getSupportsImportLc()));
        entity.setSupportsExportLc(Boolean.TRUE.equals(dto.getSupportsExportLc()));
        entity.setSupportsBtbLc(Boolean.TRUE.equals(dto.getSupportsBtbLc()));
        entity.setSupportsInlandLc(Boolean.TRUE.equals(dto.getSupportsInlandLc()));
        entity.setSupportsOnlineBanking(Boolean.TRUE.equals(dto.getSupportsOnlineBanking()));
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(bankRepository.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public BankDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<BankDTO> findAll() {
        return bankRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<BankDTO> findActiveByOrg(Long orgId) {
        return bankRepository.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<BankDTO> findLcBanksByOrg(Long orgId) {
        return bankRepository.findByOrganizationIdAndIsActiveTrueAndSupportsLcTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) { bankRepository.delete(findEntityById(id)); }

    // ── TOGGLE ────────────────────────────────────────────────────────────────

    @Override
    public BankDTO toggleStatus(Long id) {
        Bank entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(bankRepository.save(entity));
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE 1=1 "
                + CommonUtils.searchILike(search, Arrays.asList(
                        "b.bank_code", "b.bank_name", "b.bank_name_local",
                        "b.short_name", "b.swift_code", "b.bank_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY b.id DESC)       AS sl,
                COUNT(*)     OVER ()                         AS full_count,
                b.id,
                b.bank_code,
                b.bank_name,
                COALESCE(b.short_name,    '—')              AS short_name,
                COALESCE(b.swift_code,    '—')              AS swift_code,
                COALESCE(b.bank_type,     '—')              AS bank_type,
                CASE WHEN b.supports_lc THEN '<span class="badge bg-info text-dark">LC</span>' ELSE '' END AS lc_flag,
                TO_CHAR(b.created_at, 'DD-Mon-YYYY')         AS created_at,
                CASE WHEN b.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="bankShow('   || b.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="bankEdit('   || b.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="bankToggle(' || b.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="bankDelete(' || b.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                              AS actions
            FROM stp_banks b
            %s
            ORDER BY b.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public BankDTO toDTO(Bank e) {
        return BankDTO.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .bankCode(e.getBankCode())
                .bankName(e.getBankName())
                .bankNameLocal(e.getBankNameLocal())
                .shortName(e.getShortName())
                .bankType(e.getBankType() != null ? e.getBankType().name() : null)
                .bankCategory(e.getBankCategory())
                .swiftCode(e.getSwiftCode())
                .centralBankCode(e.getCentralBankCode())
                .routingNumberPrefix(e.getRoutingNumberPrefix())
                .headOfficeAddress(e.getHeadOfficeAddress())
                .headOfficeCity(e.getHeadOfficeCity())
                .headOfficeCountry(e.getHeadOfficeCountry())
                .headOfficePhone(e.getHeadOfficePhone())
                .headOfficeEmail(e.getHeadOfficeEmail())
                .website(e.getWebsite())
                .correspondentBankName(e.getCorrespondentBankName())
                .correspondentSwiftCode(e.getCorrespondentSwiftCode())
                .correspondentAccountNumber(e.getCorrespondentAccountNumber())
                .rating(e.getRating() != null ? e.getRating().name() : null)
                .supportsLc(e.isSupportsLc())
                .supportsImportLc(e.isSupportsImportLc())
                .supportsExportLc(e.isSupportsExportLc())
                .supportsBtbLc(e.isSupportsBtbLc())
                .supportsInlandLc(e.isSupportsInlandLc())
                .supportsOnlineBanking(e.isSupportsOnlineBanking())
                .active(e.isActive())
                .createdAt(e.getCreatedAt()  != null ? e.getCreatedAt().toString()  : null)
                .updatedAt(e.getUpdatedAt()  != null ? e.getUpdatedAt().toString()  : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private Bank findEntityById(Long id) {
        return bankRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bank #" + id + " not found."));
    }
}
