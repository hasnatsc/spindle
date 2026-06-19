package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.AccountsMappingDTO;
import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.repository.AccountsMappingRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
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
public class AccountsMappingServiceImpl implements AccountsMappingService {

    private final AccountsMappingRepository mappingRepo;
    private final ChartOfAccountRepository  coaRepo;
    private final JdbcTemplate              jdbcTemplate;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public AccountsMappingDTO create(AccountsMappingDTO dto) {
        Long orgId = com.asg.spindleserp.security.auth.ContextProvider.getOrganizationId();
        if (mappingRepo.findByOrganizationIdAndMappingCode(orgId, dto.getMappingCode()).isPresent()) {
            throw new IllegalArgumentException("Mapping code '" + dto.getMappingCode() + "' already exists.");
        }
        AccountsMapping entity = buildEntity(dto, new AccountsMapping());
        entity.setDetails(new ArrayList<>());
        AccountsMapping saved = mappingRepo.save(entity);
        syncDetails(saved, dto.getDetails());
        return toDTO(mappingRepo.save(saved));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public AccountsMappingDTO update(Long id, AccountsMappingDTO dto) {
        AccountsMapping entity = findEntityById(id);
        buildEntity(dto, entity);
        syncDetails(entity, dto.getDetails());
        return toDTO(mappingRepo.save(entity));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AccountsMappingDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<AccountsMappingDTO> findAll() {
        return mappingRepo.findAll().stream().map(this::toDTO).toList();
    }

    // ── TOGGLE / DELETE ───────────────────────────────────────────────────────

    @Override
    public AccountsMappingDTO toggleStatus(Long id) {
        AccountsMapping e = findEntityById(id);
        e.setActive(!e.isActive());
        return toDTO(mappingRepo.save(e));
    }

    @Override
    public void delete(Long id) {
        AccountsMapping e = findEntityById(id);
        if (e.isSystem()) throw new IllegalStateException("System mappings cannot be deleted.");
        mappingRepo.delete(e);
    }

    // ── DATATABLE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        String where = "WHERE 1=1"
                + (orgId != null ? " AND m.organization_id = " + orgId : "")
                + CommonUtils.searchILike(search, Arrays.asList(
                        "m.mapping_code", "m.mapping_name", "m.module_type", "m.transaction_type"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY m.id DESC)     AS sl,
                COUNT(*)     OVER ()                       AS full_count,
                m.id,
                m.mapping_code,
                m.mapping_name,
                m.module_type,
                m.transaction_type,
                m.voucher_type,
                COALESCE(m.voucher_prefix, '—')           AS voucher_prefix,
                (SELECT COUNT(*) FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id) AS line_count,
                TO_CHAR(m.created_at, 'DD-Mon-YYYY')      AS created_at,
                CASE WHEN m.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="mapShow('   || m.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="mapEdit('   || m.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="mapToggle(' || m.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="mapDelete(' || m.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                            AS actions
            FROM acc_mapping m
            %s
            ORDER BY m.mapping_code
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ── AJAX SEARCH ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> search(String q, int page, int pageSize) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<AccountsMapping> all = orgId != null
                ? mappingRepo.findByOrganizationIdAndModuleTypeAndIsActiveTrue(orgId, "")
                : mappingRepo.findAll();
        // load ALL active for org then filter
        if (orgId != null) {
            all = mappingRepo.findAll().stream()
                    .filter(m -> m.getOrganization() != null && m.getOrganization().getId().equals(orgId) && m.isActive())
                    .collect(Collectors.toList());
        }
        String query = q == null ? "" : q.trim().toLowerCase();
        List<AccountsMapping> filtered = query.isEmpty() ? all
                : all.stream()
                    .filter(m -> (m.getMappingCode() != null && m.getMappingCode().toLowerCase().contains(query))
                              || (m.getMappingName() != null && m.getMappingName().toLowerCase().contains(query)))
                    .collect(Collectors.toList());

        int from = (page - 1) * pageSize;
        int to   = Math.min(from + pageSize, filtered.size());
        boolean hasMore = to < filtered.size();
        List<AccountsMapping> paged = from >= filtered.size() ? List.of() : filtered.subList(from, to);

        List<Map<String, Object>> items = paged.stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",   m.getId());
            item.put("text", m.getMappingCode() + " — " + m.getMappingName());
            item.put("code", m.getMappingCode());
            item.put("name", m.getMappingName());
            item.put("moduleType", m.getModuleType());
            return item;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);
        res.put("hasMore", hasMore);
        return res;
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    @Override
    public AccountsMappingDTO toDTO(AccountsMapping e) {
        AccountsMappingDTO d = AccountsMappingDTO.builder()
                .id(e.getId())
                .mappingCode(e.getMappingCode())
                .mappingName(e.getMappingName())
                .moduleType(e.getModuleType())
                .transactionType(e.getTransactionType())
                .description(e.getDescription())
                .defaultVoucherType(e.getDefaultVoucherType())
                .voucherType(e.getVoucherType())
                .voucherPrefix(e.getVoucherPrefix())
                .debitControlType(e.getDebitControlType())
                .creditControlType(e.getCreditControlType())
                .defaultNarrationTemplate(e.getDefaultNarrationTemplate())
                .useSubLedger(e.isUseSubLedger())
                .updateSubAccountBalance(e.isUpdateSubAccountBalance())
                .allowPartialPosting(e.isAllowPartialPosting())
                .autoPost(e.isAutoPost())
                .consolidateEntries(e.isConsolidateEntries())
                .createReversingEntry(e.isCreateReversingEntry())
                .requireApproval(e.isRequireApproval())
                .active(e.isActive())
                .isDefault(e.isDefault())
                .isSystem(e.isSystem())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
        // COA FK displays
        if (e.getDefaultDebitAccount()  != null) { d.setDefaultDebitAccountId(e.getDefaultDebitAccount().getId());  d.setDefaultDebitAccountDisplay(e.getDefaultDebitAccount().getAccountCode() + " — " + e.getDefaultDebitAccount().getAccountName()); }
        if (e.getDefaultCreditAccount() != null) { d.setDefaultCreditAccountId(e.getDefaultCreditAccount().getId()); d.setDefaultCreditAccountDisplay(e.getDefaultCreditAccount().getAccountCode() + " — " + e.getDefaultCreditAccount().getAccountName()); }
        if (e.getDiscountAccount()  != null) { d.setDiscountAccountId(e.getDiscountAccount().getId());  d.setDiscountAccountDisplay(e.getDiscountAccount().getAccountCode() + " — " + e.getDiscountAccount().getAccountName()); }
        if (e.getFreightAccount()   != null) { d.setFreightAccountId(e.getFreightAccount().getId());   d.setFreightAccountDisplay(e.getFreightAccount().getAccountCode() + " — " + e.getFreightAccount().getAccountName()); }
        if (e.getInputVatAccount()  != null) { d.setInputVatAccountId(e.getInputVatAccount().getId());  d.setInputVatAccountDisplay(e.getInputVatAccount().getAccountCode() + " — " + e.getInputVatAccount().getAccountName()); }
        if (e.getOutputVatAccount() != null) { d.setOutputVatAccountId(e.getOutputVatAccount().getId()); d.setOutputVatAccountDisplay(e.getOutputVatAccount().getAccountCode() + " — " + e.getOutputVatAccount().getAccountName()); }
        if (e.getForexGainAccount() != null) { d.setForexGainAccountId(e.getForexGainAccount().getId()); d.setForexGainAccountDisplay(e.getForexGainAccount().getAccountCode() + " — " + e.getForexGainAccount().getAccountName()); }
        if (e.getForexLossAccount() != null) { d.setForexLossAccountId(e.getForexLossAccount().getId()); d.setForexLossAccountDisplay(e.getForexLossAccount().getAccountCode() + " — " + e.getForexLossAccount().getAccountName()); }
        if (e.getTdsAccount()       != null) { d.setTdsAccountId(e.getTdsAccount().getId());       d.setTdsAccountDisplay(e.getTdsAccount().getAccountCode() + " — " + e.getTdsAccount().getAccountName()); }
        if (e.getAitAccount()       != null) { d.setAitAccountId(e.getAitAccount().getId());       d.setAitAccountDisplay(e.getAitAccount().getAccountCode() + " — " + e.getAitAccount().getAccountName()); }
        if (e.getRoundingAccount()  != null) { d.setRoundingAccountId(e.getRoundingAccount().getId()); d.setRoundingAccountDisplay(e.getRoundingAccount().getAccountCode() + " — " + e.getRoundingAccount().getAccountName()); }
        // Details
        if (e.getDetails() != null) {
            d.setDetails(e.getDetails().stream().map(det -> {
                AccountsMappingDTO.DetailDTO dd = new AccountsMappingDTO.DetailDTO();
                dd.setId(det.getId());
                dd.setLineNumber(det.getLineNumber());
                dd.setEntryType(det.getEntryType() != null ? det.getEntryType().name() : null);
                dd.setAmountType(det.getAmountType());
                dd.setPercentage(det.getPercentage());
                dd.setFixedAmount(det.getFixedAmount());
                dd.setFormula(det.getFormula());
                dd.setFieldReference(det.getFieldReference());
                dd.setEntryName(det.getEntryName());
                dd.setEntryDescription(det.getEntryDescription());
                dd.setLineNarration(det.getLineNarration());
                dd.setCondition(det.getCondition());
                dd.setConditionOperator(det.getConditionOperator());
                dd.setConditionValue(det.getConditionValue());
                dd.setControlAccountType(det.getControlAccountType());
                dd.setTaxCode(det.getTaxCode());
                dd.setTaxRate(det.getTaxRate());
                dd.setSortOrder(det.getSortOrder());
                dd.setNegateAmount(det.isNegateAmount());
                dd.setRoundAmount(det.isRoundAmount());
                dd.setSkipIfZero(det.isSkipIfZero());
                dd.setIsTaxEntry(det.isTaxEntry());
                dd.setIsOptional(det.isOptional());
                dd.setActive(det.isActive());
                dd.setInheritCostCenter(det.isInheritCostCenter());
                if (det.getAccount() != null) { dd.setAccountId(det.getAccount().getId()); dd.setAccountDisplay(det.getAccount().getAccountCode() + " — " + det.getAccount().getAccountName()); }
                return dd;
            }).toList());
        }
        return d;
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private AccountsMapping buildEntity(AccountsMappingDTO dto, AccountsMapping e) {
        e.setMappingCode(dto.getMappingCode().trim().toUpperCase());
        e.setMappingName(dto.getMappingName().trim());
        e.setModuleType(dto.getModuleType());
        e.setTransactionType(dto.getTransactionType());
        e.setDescription(dto.getDescription());
        e.setDefaultVoucherType(dto.getDefaultVoucherType());
        e.setVoucherType(dto.getVoucherType());
        e.setVoucherPrefix(dto.getVoucherPrefix());
        e.setDebitControlType(dto.getDebitControlType() != null ? dto.getDebitControlType() : "NONE");
        e.setCreditControlType(dto.getCreditControlType() != null ? dto.getCreditControlType() : "NONE");
        e.setDefaultNarrationTemplate(dto.getDefaultNarrationTemplate());
        e.setUseSubLedger(Boolean.TRUE.equals(dto.getUseSubLedger()));
        e.setUpdateSubAccountBalance(Boolean.TRUE.equals(dto.getUpdateSubAccountBalance()));
        e.setAllowPartialPosting(Boolean.TRUE.equals(dto.getAllowPartialPosting()));
        e.setAutoPost(Boolean.TRUE.equals(dto.getAutoPost()));
        e.setConsolidateEntries(Boolean.TRUE.equals(dto.getConsolidateEntries()));
        e.setCreateReversingEntry(Boolean.TRUE.equals(dto.getCreateReversingEntry()));
        e.setRequireApproval(Boolean.TRUE.equals(dto.getRequireApproval()));
        e.setActive(Boolean.TRUE.equals(dto.getActive()));
        e.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        // COA FKs
        e.setDefaultDebitAccount(dto.getDefaultDebitAccountId() != null ? coaRepo.getReferenceById(dto.getDefaultDebitAccountId()) : null);
        e.setDefaultCreditAccount(dto.getDefaultCreditAccountId() != null ? coaRepo.getReferenceById(dto.getDefaultCreditAccountId()) : null);
        e.setDiscountAccount(dto.getDiscountAccountId() != null ? coaRepo.getReferenceById(dto.getDiscountAccountId()) : null);
        e.setFreightAccount(dto.getFreightAccountId() != null ? coaRepo.getReferenceById(dto.getFreightAccountId()) : null);
        e.setInputVatAccount(dto.getInputVatAccountId() != null ? coaRepo.getReferenceById(dto.getInputVatAccountId()) : null);
        e.setOutputVatAccount(dto.getOutputVatAccountId() != null ? coaRepo.getReferenceById(dto.getOutputVatAccountId()) : null);
        e.setForexGainAccount(dto.getForexGainAccountId() != null ? coaRepo.getReferenceById(dto.getForexGainAccountId()) : null);
        e.setForexLossAccount(dto.getForexLossAccountId() != null ? coaRepo.getReferenceById(dto.getForexLossAccountId()) : null);
        e.setTdsAccount(dto.getTdsAccountId() != null ? coaRepo.getReferenceById(dto.getTdsAccountId()) : null);
        e.setAitAccount(dto.getAitAccountId() != null ? coaRepo.getReferenceById(dto.getAitAccountId()) : null);
        e.setRoundingAccount(dto.getRoundingAccountId() != null ? coaRepo.getReferenceById(dto.getRoundingAccountId()) : null);
        return e;
    }

    private void syncDetails(AccountsMapping parent, List<AccountsMappingDTO.DetailDTO> dtos) {
        if (dtos == null) return;
        parent.getDetails().clear();
        int lineNum = 1;
        for (AccountsMappingDTO.DetailDTO dd : dtos) {
            AccountsMappingDetail det = new AccountsMappingDetail();
            det.setAccountsMapping(parent);
            det.setLineNumber(lineNum++);
            det.setEntryType(JournalEntryLine.EntryType.valueOf(dd.getEntryType()));
            det.setAmountType(dd.getAmountType());
            det.setPercentage(dd.getPercentage());
            det.setFixedAmount(dd.getFixedAmount());
            det.setFormula(dd.getFormula());
            det.setFieldReference(dd.getFieldReference());
            det.setEntryName(dd.getEntryName());
            det.setEntryDescription(dd.getEntryDescription());
            det.setLineNarration(dd.getLineNarration());
            det.setCondition(dd.getCondition());
            det.setConditionOperator(dd.getConditionOperator());
            det.setConditionValue(dd.getConditionValue());
            det.setControlAccountType(dd.getControlAccountType() != null ? dd.getControlAccountType() : "NONE");
            det.setTaxCode(dd.getTaxCode());
            det.setTaxRate(dd.getTaxRate());
            det.setSortOrder(dd.getSortOrder() != null ? dd.getSortOrder() : 0);
            det.setNegateAmount(Boolean.TRUE.equals(dd.getNegateAmount()));
            det.setRoundAmount(Boolean.TRUE.equals(dd.getRoundAmount()));
            det.setSkipIfZero(Boolean.TRUE.equals(dd.getSkipIfZero()));
            det.setTaxEntry(Boolean.TRUE.equals(dd.getIsTaxEntry()));
            det.setOptional(Boolean.TRUE.equals(dd.getIsOptional()));
            det.setActive(Boolean.TRUE.equals(dd.getActive()));
            det.setInheritCostCenter(Boolean.TRUE.equals(dd.getInheritCostCenter()));
            if (dd.getAccountId() != null) det.setAccount(coaRepo.getReferenceById(dd.getAccountId()));
            parent.getDetails().add(det);
        }
    }

    private AccountsMapping findEntityById(Long id) {
        return mappingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping #" + id + " not found."));
    }
}
