package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.ChartOfAccountDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChartOfAccountServiceImpl implements ChartOfAccountService {

    private final ChartOfAccountRepository coaRepo;
    private final JdbcTemplate             jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountDTO create(ChartOfAccountDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();

        if (coaRepo.existsByOrganizationIdAndAccountCode(orgId, dto.getAccountCode())) {
            throw new IllegalArgumentException("Account code '" + dto.getAccountCode() + "' already exists.");
        }

        ChartOfAccount entity = ChartOfAccount.builder()
                .accountCode(dto.getAccountCode().trim().toUpperCase())
                .accountName(dto.getAccountName().trim())
                .accountType(ChartOfAccount.AccountType.valueOf(dto.getAccountType()))
                .accountNature(ChartOfAccount.AccountNature.valueOf(dto.getAccountNature()))
                .level(resolveLevel(dto.getParentAccountId()))
                .openingBalance(dto.getOpeningBalance())
                .currency(dto.getCurrency())
                .description(dto.getDescription())
                .taxId(dto.getTaxId())
                .isActive(Boolean.TRUE.equals(dto.getActive()))
                .isSystem(Boolean.TRUE.equals(dto.getIsSystem()))
                .isControlAccount(Boolean.TRUE.equals(dto.getIsControlAccount()))
                .allowManualEntry(!Boolean.FALSE.equals(dto.getAllowManualEntry()))
                .build();

        if (dto.getParentAccountId() != null) {
            entity.setParentAccount(coaRepo.getReferenceById(dto.getParentAccountId()));
        }

        return toDTO(coaRepo.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountDTO update(Long id, ChartOfAccountDTO dto) {
        ChartOfAccount entity = findEntityById(id);
        Long orgId = ContextProvider.getOrganizationId();

        if (!entity.getAccountCode().equalsIgnoreCase(dto.getAccountCode())
                && coaRepo.existsByOrganizationIdAndAccountCode(orgId, dto.getAccountCode())) {
            throw new IllegalArgumentException("Account code '" + dto.getAccountCode() + "' already exists.");
        }

        entity.setAccountCode(dto.getAccountCode().trim().toUpperCase());
        entity.setAccountName(dto.getAccountName().trim());
        entity.setAccountType(ChartOfAccount.AccountType.valueOf(dto.getAccountType()));
        entity.setAccountNature(ChartOfAccount.AccountNature.valueOf(dto.getAccountNature()));
        entity.setDescription(dto.getDescription());
        entity.setTaxId(dto.getTaxId());
        entity.setCurrency(dto.getCurrency());
        entity.setOpeningBalance(dto.getOpeningBalance());
        entity.setActive(Boolean.TRUE.equals(dto.getActive()));
        entity.setControlAccount(Boolean.TRUE.equals(dto.getIsControlAccount()));
        entity.setAllowManualEntry(!Boolean.FALSE.equals(dto.getAllowManualEntry()));

        if (dto.getParentAccountId() != null) {
            if (dto.getParentAccountId().equals(id)) {
                throw new IllegalArgumentException("An account cannot be its own parent.");
            }
            entity.setParentAccount(coaRepo.getReferenceById(dto.getParentAccountId()));
            entity.setLevel(resolveLevel(dto.getParentAccountId()));
        } else {
            entity.setParentAccount(null);
            entity.setLevel(1);
        }

        return toDTO(coaRepo.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ChartOfAccountDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountDTO> findAll() {
        return coaRepo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountDTO> findActiveByOrg(Long orgId) {
        return coaRepo.findByOrganizationIdAndIsActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE / DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountDTO toggleStatus(Long id) {
        ChartOfAccount entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(coaRepo.save(entity));
    }

    @Override
    public void delete(Long id) {
        coaRepo.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE  (JdbcTemplate — mirrors BusinessUnitService)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        String where = "WHERE 1=1"
                + (orgId != null ? " AND c.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList(
                        "c.account_code", "c.account_name",
                        "c.account_type", "c.account_nature"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.id DESC)         AS sl,
                COUNT(*)     OVER ()                           AS full_count,
                c.id,
                c.account_code,
                c.account_name,
                c.account_type,
                c.account_nature,
                c.level,
                COALESCE(p.account_code || ' — ' || p.account_name, '—') AS parent_account,
                COALESCE(c.current_balance::text, '—')         AS current_balance,
                TO_CHAR(c.created_at, 'DD-Mon-YYYY')           AS created_at,
                CASE WHEN c.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="coaShow('   || c.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="coaEdit('   || c.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="coaToggle(' || c.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="coaDelete(' || c.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                AS actions
            FROM acc_chart_of_accounts c
            LEFT JOIN acc_chart_of_accounts p ON p.id = c.parent_account_id
            %s
            ORDER BY c.account_code ASC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AJAX SELECT2 SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> search(String q, int page, int pageSize) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        List<ChartOfAccount> all = orgId != null
                ? coaRepo.findByOrganizationIdAndIsActiveTrue(orgId)
                : coaRepo.findAll();

        String query = q == null ? "" : q.trim().toLowerCase();
        List<ChartOfAccount> filtered = query.isEmpty() ? all
                : all.stream()
                    .filter(c -> (c.getAccountCode() != null && c.getAccountCode().toLowerCase().contains(query))
                              || (c.getAccountName() != null && c.getAccountName().toLowerCase().contains(query)))
                    .collect(Collectors.toList());

        int from    = (page - 1) * pageSize;
        int to      = Math.min(from + pageSize, filtered.size());
        boolean hasMore = to < filtered.size();
        List<ChartOfAccount> paged = from >= filtered.size() ? List.of() : filtered.subList(from, to);

        List<Map<String, Object>> items = paged.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          c.getId());
            m.put("text",        c.getAccountCode() + " — " + c.getAccountName());
            m.put("code",        c.getAccountCode());
            m.put("name",        c.getAccountName());
            m.put("accountType", c.getAccountType() != null ? c.getAccountType().name() : "");
            m.put("nature",      c.getAccountNature() != null ? c.getAccountNature().name() : "");
            return m;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items",   items);
        res.put("hasMore", hasMore);
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountDTO toDTO(ChartOfAccount e) {
        return ChartOfAccountDTO.builder()
                .id(e.getId())
                .parentAccountId(e.getParentAccount() != null ? e.getParentAccount().getId() : null)
                .parentAccountDisplay(e.getParentAccount() != null
                        ? e.getParentAccount().getAccountCode() + " — " + e.getParentAccount().getAccountName() : null)
                .accountCode(e.getAccountCode())
                .accountName(e.getAccountName())
                .accountType(e.getAccountType() != null ? e.getAccountType().name() : null)
                .accountNature(e.getAccountNature() != null ? e.getAccountNature().name() : null)
                .level(e.getLevel())
                .openingBalance(e.getOpeningBalance())
                .currentBalance(e.getCurrentBalance())
                .currency(e.getCurrency())
                .description(e.getDescription())
                .taxId(e.getTaxId())
                .active(e.isActive())
                .isSystem(e.isSystem())
                .isControlAccount(e.isControlAccount())
                .allowManualEntry(e.isAllowManualEntry())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ChartOfAccount findEntityById(Long id) {
        return coaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account #" + id + " not found."));
    }

    private int resolveLevel(Long parentId) {
        if (parentId == null) return 1;
        return coaRepo.findById(parentId).map(p -> p.getLevel() + 1).orElse(1);
    }
}
