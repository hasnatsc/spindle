package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.AccountsPolicyDTO;
import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.repository.*;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
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
public class AccountsPolicyServiceImpl implements AccountsPolicyService {

    private final AccountsPolicyRepository  policyRepo;
    private final AccountsMappingRepository mappingRepo;
    private final ChartOfAccountRepository  coaRepo;
    private final JdbcTemplate              jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public AccountsPolicyDTO create(AccountsPolicyDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        if (policyRepo.findByOrganizationIdAndPolicyCode(orgId, dto.getPolicyCode()).isPresent()) {
            throw new IllegalArgumentException("Policy code '" + dto.getPolicyCode() + "' already exists.");
        }
        return toDTO(policyRepo.save(buildEntity(dto, new AccountsPolicy())));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public AccountsPolicyDTO update(Long id, AccountsPolicyDTO dto) {
        AccountsPolicy entity = findEntityById(id);
        if (entity.isSystem()) throw new IllegalStateException("System policies cannot be edited.");
        return toDTO(policyRepo.save(buildEntity(dto, entity)));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AccountsPolicyDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<AccountsPolicyDTO> findAll() {
        return policyRepo.findAll().stream().map(this::toDTO).toList();
    }

    // ── TOGGLE / DELETE ───────────────────────────────────────────────────────

    @Override
    public AccountsPolicyDTO toggleStatus(Long id) {
        AccountsPolicy e = findEntityById(id);
        e.setActive(!e.isActive());
        return toDTO(policyRepo.save(e));
    }

    @Override
    public void delete(Long id) {
        AccountsPolicy e = findEntityById(id);
        if (e.isSystem()) throw new IllegalStateException("System policies cannot be deleted.");
        policyRepo.delete(e);
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        String where = "WHERE 1=1"
                + (orgId != null ? " AND p.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList(
                        "p.policy_code", "p.policy_name", "p.policy_type", "p.module_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY p.id DESC)     AS sl,
                COUNT(*)     OVER ()                       AS full_count,
                p.id,
                p.policy_code,
                p.policy_name,
                p.policy_type,
                COALESCE(p.module_type, '—')              AS module_type,
                COALESCE(p.voucher_prefix, '—')           AS voucher_prefix,
                CASE WHEN p.is_default THEN '<span class="badge bg-info text-dark">Default</span>' ELSE '' END AS is_default_badge,
                CASE WHEN p.is_system  THEN '<span class="badge bg-secondary">System</span>'        ELSE '' END AS is_system_badge,
                TO_CHAR(p.created_at, 'DD-Mon-YYYY')      AS created_at,
                CASE WHEN p.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="policyShow('   || p.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="policyEdit('   || p.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="policyToggle(' || p.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="policyDelete(' || p.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                            AS actions
            FROM acc_policy p
            %s
            ORDER BY p.policy_code
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public AccountsPolicyDTO toDTO(AccountsPolicy e) {
        AccountsPolicyDTO d = AccountsPolicyDTO.builder()
                .id(e.getId())
                .policyCode(e.getPolicyCode())
                .policyName(e.getPolicyName())
                .policyType(e.getPolicyType())
                .moduleType(e.getModuleType())
                .description(e.getDescription())
                .voucherPrefix(e.getVoucherPrefix())
                .nextVoucherNumber(e.getNextVoucherNumber())
                .numberPadding(e.getNumberPadding())
                .approvalThreshold(e.getApprovalThreshold())
                .minimumAmount(e.getMinimumAmount())
                .maximumAmount(e.getMaximumAmount())
                .defaultNarrationTemplate(e.getDefaultNarrationTemplate())
                .backdatingDays(e.getBackdatingDays())
                .autoNumbering(e.isAutoNumbering())
                .autoPost(e.isAutoPost())
                .requireApproval(e.isRequireApproval())
                .allowBackdating(e.isAllowBackdating())
                .allowFutureDating(e.isAllowFutureDating())
                .allowEditAfterPost(e.isAllowEditAfterPost())
                .allowReversal(e.isAllowReversal())
                .allowNegativeAmount(e.isAllowNegativeAmount())
                .allowZeroAmount(e.isAllowZeroAmount())
                .active(e.isActive())
                .isDefault(e.isDefault())
                .isSystem(e.isSystem())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
        if (e.getAccountsMapping() != null) {
            d.setAccountsMappingId(e.getAccountsMapping().getId());
            d.setAccountsMappingDisplay(e.getAccountsMapping().getMappingCode() + " — " + e.getAccountsMapping().getMappingName());
        }
        if (e.getDefaultDebitAccount() != null) {
            d.setDefaultDebitAccountId(e.getDefaultDebitAccount().getId());
            d.setDefaultDebitAccountDisplay(e.getDefaultDebitAccount().getAccountCode() + " — " + e.getDefaultDebitAccount().getAccountName());
        }
        if (e.getDefaultCreditAccount() != null) {
            d.setDefaultCreditAccountId(e.getDefaultCreditAccount().getId());
            d.setDefaultCreditAccountDisplay(e.getDefaultCreditAccount().getAccountCode() + " — " + e.getDefaultCreditAccount().getAccountName());
        }
        return d;
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private AccountsPolicy buildEntity(AccountsPolicyDTO dto, AccountsPolicy e) {
        e.setPolicyCode(dto.getPolicyCode().trim().toUpperCase());
        e.setPolicyName(dto.getPolicyName().trim());
        e.setPolicyType(dto.getPolicyType());
        e.setModuleType(dto.getModuleType());
        e.setDescription(dto.getDescription());
        e.setVoucherPrefix(dto.getVoucherPrefix());
        e.setNextVoucherNumber(dto.getNextVoucherNumber());
        e.setNumberPadding(dto.getNumberPadding() != null ? dto.getNumberPadding() : 6);
        e.setApprovalThreshold(dto.getApprovalThreshold());
        e.setMinimumAmount(dto.getMinimumAmount());
        e.setMaximumAmount(dto.getMaximumAmount());
        e.setDefaultNarrationTemplate(dto.getDefaultNarrationTemplate());
        e.setBackdatingDays(dto.getBackdatingDays());
        e.setAutoNumbering(!Boolean.FALSE.equals(dto.getAutoNumbering()));
        e.setAutoPost(Boolean.TRUE.equals(dto.getAutoPost()));
        e.setRequireApproval(Boolean.TRUE.equals(dto.getRequireApproval()));
        e.setAllowBackdating(Boolean.TRUE.equals(dto.getAllowBackdating()));
        e.setAllowFutureDating(Boolean.TRUE.equals(dto.getAllowFutureDating()));
        e.setAllowEditAfterPost(Boolean.TRUE.equals(dto.getAllowEditAfterPost()));
        e.setAllowReversal(!Boolean.FALSE.equals(dto.getAllowReversal()));
        e.setAllowNegativeAmount(Boolean.TRUE.equals(dto.getAllowNegativeAmount()));
        e.setAllowZeroAmount(Boolean.TRUE.equals(dto.getAllowZeroAmount()));
        e.setActive(Boolean.TRUE.equals(dto.getActive()));
        e.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        e.setAccountsMapping(dto.getAccountsMappingId() != null ? mappingRepo.getReferenceById(dto.getAccountsMappingId()) : null);
        e.setDefaultDebitAccount(dto.getDefaultDebitAccountId() != null ? coaRepo.getReferenceById(dto.getDefaultDebitAccountId()) : null);
        e.setDefaultCreditAccount(dto.getDefaultCreditAccountId() != null ? coaRepo.getReferenceById(dto.getDefaultCreditAccountId()) : null);
        return e;
    }

    private AccountsPolicy findEntityById(Long id) {
        return policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy #" + id + " not found."));
    }
}
