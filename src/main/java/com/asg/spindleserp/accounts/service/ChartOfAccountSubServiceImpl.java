package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.repository.BankRepository;
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
public class ChartOfAccountSubServiceImpl implements ChartOfAccountSubService {

    private final ChartOfAccountSubRepository subRepo;
    private final ChartOfAccountRepository    coaRepo;
    private final BankRepository              bankRepo;
    private final JdbcTemplate                jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountSubDTO create(ChartOfAccountSubDTO dto) {
        ChartOfAccountSub entity = buildEntity(dto, null);
        return toDTO(subRepo.save(entity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountSubDTO update(Long id, ChartOfAccountSubDTO dto) {
        ChartOfAccountSub existing = findEntityById(id);
        ChartOfAccountSub updated  = buildEntity(dto, existing);
        return toDTO(subRepo.save(updated));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ChartOfAccountSubDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountSubDTO> findAll() {
        return subRepo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountSubDTO> findByType(String subAccountType) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        return subRepo.findByOrganizationIdAndIsActiveTrue(orgId).stream()
                .filter(s -> s.getSubAccountType() != null
                        && s.getSubAccountType().name().equalsIgnoreCase(subAccountType))
                .map(this::toDTO).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE / DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChartOfAccountSubDTO toggleStatus(Long id) {
        ChartOfAccountSub entity = findEntityById(id);
        entity.setActive(!entity.isActive());
        return toDTO(subRepo.save(entity));
    }

    @Override
    public void delete(Long id) {
        subRepo.delete(findEntityById(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(String subAccountType, int draw, int start, int length, String search) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        String typeFilter = (subAccountType != null && !subAccountType.isBlank())
                ? " AND s.sub_account_type = '" + subAccountType.toUpperCase() + "'" : "";

        String where = "WHERE 1=1"
                + (orgId != null ? " AND s.organization_id = " + orgId : "")
                + typeFilter
                + CommonUtils.searchILike(search, Arrays.asList(
                        "s.sub_account_code", "s.sub_account_name",
                        "s.sub_account_type", "s.contact_person",
                        "s.contact_phone", "c.account_code", "c.account_name"));

        String fnPrefix = fnPrefix(subAccountType);

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY s.id DESC)           AS sl,
                COUNT(*)     OVER ()                             AS full_count,
                s.id,
                s.sub_account_type,
                s.sub_account_code,
                s.sub_account_name,
                c.account_code || ' — ' || c.account_name        AS main_account,
                COALESCE(s.contact_person, '—')                  AS contact_person,
                COALESCE(s.contact_phone, '—')                   AS contact_phone,
                COALESCE(s.currency, '—')                        AS currency,
                COALESCE(s.current_balance::text, '—')           AS current_balance,
                TO_CHAR(s.created_at, 'DD-Mon-YYYY')             AS created_at,
                CASE WHEN s.is_active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="%1$sShow('   || s.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="%1$sEdit('   || s.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="%1$sToggle(' || s.id || ')" class="btn btn-white btn-sm" title="Toggle"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="%1$sDelete(' || s.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                                   AS actions
            FROM acc_chart_of_accounts_sub s
            JOIN acc_chart_of_accounts c ON c.id = s.main_account_id
            %2$s
            ORDER BY s.sub_account_code ASC
            OFFSET %3$d LIMIT %4$d
            """, fnPrefix, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AJAX SELECT2 SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> search(String q, String subAccountType, int page, int pageSize) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);

        List<ChartOfAccountSub> all = orgId != null
                ? subRepo.findByOrganizationIdAndIsActiveTrue(orgId)
                : subRepo.findAll();

        if (subAccountType != null && !subAccountType.isBlank()) {
            all = all.stream()
                    .filter(s -> s.getSubAccountType() != null
                            && s.getSubAccountType().name().equalsIgnoreCase(subAccountType))
                    .collect(Collectors.toList());
        }

        String query = q == null ? "" : q.trim().toLowerCase();
        List<ChartOfAccountSub> filtered = query.isEmpty() ? all
                : all.stream()
                    .filter(s -> (s.getSubAccountCode() != null && s.getSubAccountCode().toLowerCase().contains(query))
                              || (s.getSubAccountName() != null && s.getSubAccountName().toLowerCase().contains(query)))
                    .collect(Collectors.toList());

        int from    = (page - 1) * pageSize;
        int to      = Math.min(from + pageSize, filtered.size());
        boolean hasMore = to < filtered.size();
        List<ChartOfAccountSub> paged = from >= filtered.size() ? List.of() : filtered.subList(from, to);

        List<Map<String, Object>> items = paged.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             s.getId());
            m.put("text",           s.getSubAccountCode() + " — " + s.getSubAccountName());
            m.put("code",           s.getSubAccountCode());
            m.put("name",           s.getSubAccountName());
            m.put("subAccountType", s.getSubAccountType() != null ? s.getSubAccountType().name() : "");
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
    public ChartOfAccountSubDTO toDTO(ChartOfAccountSub e) {
        ChartOfAccountSubDTO d = new ChartOfAccountSubDTO();
        d.setId(e.getId());
        d.setMainAccountId(e.getMainAccount() != null ? e.getMainAccount().getId() : null);
        d.setMainAccountDisplay(e.getMainAccount() != null
                ? e.getMainAccount().getAccountCode() + " — " + e.getMainAccount().getAccountName() : null);
        d.setSubAccountType(e.getSubAccountType() != null ? e.getSubAccountType().name() : null);
        d.setSubAccountCode(e.getSubAccountCode());
        d.setSubAccountName(e.getSubAccountName());
        d.setActive(e.isActive());
        d.setContactPerson(e.getContactPerson());
        d.setContactPhone(e.getContactPhone());
        d.setContactEmail(e.getContactEmail());
        d.setAddress(e.getAddress());
        d.setCity(e.getCity());
        d.setState(e.getState());
        d.setCountry(e.getCountry());
        d.setPostalCode(e.getPostalCode());
        d.setTaxId(e.getTaxId());
        d.setVatRegistrationNo(e.getVatRegistrationNo());
        d.setCurrency(e.getCurrency());
        d.setDescription(e.getDescription());
        d.setRemarks(e.getRemarks());
        d.setOpeningBalance(e.getOpeningBalance());
        d.setCurrentBalance(e.getCurrentBalance());
        // Bank
        d.setBankId(e.getBank() != null ? e.getBank().getId() : null);
        d.setBankName(e.getBank() != null ? e.getBank().getBankName() : null);
        d.setBankAccountCode(e.getBankAccountCode());
        d.setAccountNumber(e.getAccountNumber());
        d.setAccountTitle(e.getAccountTitle());
        d.setBankNameManual(e.getBankName());
        d.setBankAccountType(e.getBankAccountType());
        d.setBranchName(e.getBranchName());
        d.setBranchCode(e.getBranchCode());
        d.setBranchAddress(e.getBranchAddress());
        d.setBranchPhone(e.getBranchPhone());
        d.setRoutingNumber(e.getRoutingNumber());
        d.setSwiftCode(e.getSwiftCode());
        d.setIbanNumber(e.getIbanNumber());
        d.setInterestRate(e.getInterestRate());
        d.setOverdraftLimit(e.getOverdraftLimit());
        d.setOverdraftInterestRate(e.getOverdraftInterestRate());
        // Cash
        d.setCashAccountCode(e.getCashAccountCode());
        d.setCashAccountType(e.getCashAccountType());
        d.setLocation(e.getLocation());
        d.setCustodian(e.getCustodian());
        d.setCustodianEmail(e.getCustodianEmail());
        d.setCustodianPhone(e.getCustodianPhone());
        d.setMaximumLimit(e.getMaximumLimit());
        d.setMinimumLimit(e.getMinimumLimit());
        d.setApprovalLimit(e.getApprovalLimit());
        d.setRequiresApproval(e.isRequiresApproval());
        // Customer
        d.setCustomerCode(e.getCustomerCode());
        d.setCreditLimit(e.getCreditLimit());
        d.setPaymentTerms(e.getPaymentTerms());
        d.setCreditDays(e.getCreditDays());
        d.setSalesRepresentative(e.getSalesRepresentative());
        d.setCustomerGroup(e.getCustomerGroup());
        d.setLoyaltyPoints(e.getLoyaltyPoints());
        d.setIsExportCustomer(e.getIsExportCustomer());
        // Supplier
        d.setSupplierCode(e.getSupplierCode());
        d.setLeadTimeDays(e.getLeadTimeDays());
        d.setPreferredCurrency(e.getPreferredCurrency());
        d.setIsImportSupplier(e.getIsImportSupplier());
        // LC
        d.setLcNumber(e.getLcNumber());
        d.setManualLcNumber(e.getManualLcNumber());
        d.setLcType(e.getLcType());
        d.setLcStatus(e.getLcStatus());
        d.setTransactionCurrency(e.getTransactionCurrency());
        d.setLcAmount(e.getLcAmount());
        d.setExchangeRate(e.getExchangeRate());
        d.setIssueDate(e.getIssueDate());
        d.setExpiryDate(e.getExpiryDate());
        d.setShipmentDate(e.getShipmentDate());
        d.setReceivingDate(e.getReceivingDate());
        d.setTenureDays(e.getTenureDays());
        d.setMasterLcNo(e.getMasterLcNo());
        d.setBtbLcNo(e.getBtbLcNo());
        d.setMasterLcDate(e.getMasterLcDate());
        d.setPaymentTerm(e.getPaymentTerm());
        d.setShipmentMode(e.getShipmentMode());
        d.setPartialShipmentAllowed(e.getPartialShipmentAllowed());
        d.setBtmaCertificateRequired(e.getBtmaCertificateRequired());
        // Audit
        d.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        d.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedBy(e.getUpdatedBy());
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ChartOfAccountSub buildEntity(ChartOfAccountSubDTO dto, ChartOfAccountSub existing) {
        String type = dto.getSubAccountType().toUpperCase();
        ChartOfAccountSub entity = (existing != null) ? existing : instantiate(type);

        // Main account FK
        if (dto.getMainAccountId() != null) {
            entity.setMainAccount(coaRepo.getReferenceById(dto.getMainAccountId()));
        }

        entity.setSubAccountCode(dto.getSubAccountCode().trim().toUpperCase());
        entity.setSubAccountName(dto.getSubAccountName().trim());
        entity.setSubAccountType(ChartOfAccountSub.SubAccountType.valueOf(type));
        entity.setActive(Boolean.TRUE.equals(dto.getActive()));
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry());
        entity.setPostalCode(dto.getPostalCode());
        entity.setTaxId(dto.getTaxId());
        entity.setVatRegistrationNo(dto.getVatRegistrationNo());
        entity.setCurrency(dto.getCurrency());
        entity.setDescription(dto.getDescription());
        entity.setRemarks(dto.getRemarks());
        entity.setOpeningBalance(dto.getOpeningBalance());

        switch (type) {
            case "BANK" -> {
                if (dto.getBankId() != null) entity.setBank(bankRepo.getReferenceById(dto.getBankId()));
                entity.setBankAccountCode(dto.getBankAccountCode());
                entity.setAccountNumber(dto.getAccountNumber());
                entity.setAccountTitle(dto.getAccountTitle());
                entity.setBankName(dto.getBankNameManual());
                entity.setBankAccountType(dto.getBankAccountType());
                entity.setBranchName(dto.getBranchName());
                entity.setBranchCode(dto.getBranchCode());
                entity.setBranchAddress(dto.getBranchAddress());
                entity.setBranchPhone(dto.getBranchPhone());
                entity.setRoutingNumber(dto.getRoutingNumber());
                entity.setSwiftCode(dto.getSwiftCode());
                entity.setIbanNumber(dto.getIbanNumber());
                entity.setInterestRate(dto.getInterestRate());
                entity.setOverdraftLimit(dto.getOverdraftLimit());
                entity.setOverdraftInterestRate(dto.getOverdraftInterestRate());
            }
            case "CASH" -> {
                entity.setCashAccountCode(dto.getCashAccountCode());
                entity.setCashAccountType(dto.getCashAccountType());
                entity.setLocation(dto.getLocation());
                entity.setCustodian(dto.getCustodian());
                entity.setCustodianEmail(dto.getCustodianEmail());
                entity.setCustodianPhone(dto.getCustodianPhone());
                entity.setMaximumLimit(dto.getMaximumLimit());
                entity.setMinimumLimit(dto.getMinimumLimit());
                entity.setApprovalLimit(dto.getApprovalLimit());
                entity.setRequiresApproval(Boolean.TRUE.equals(dto.getRequiresApproval()));
            }
            case "CUSTOMER" -> {
                entity.setCustomerCode(dto.getCustomerCode());
                entity.setCreditLimit(dto.getCreditLimit());
                entity.setPaymentTerms(dto.getPaymentTerms());
                entity.setCreditDays(dto.getCreditDays());
                entity.setSalesRepresentative(dto.getSalesRepresentative());
                entity.setCustomerGroup(dto.getCustomerGroup());
                entity.setLoyaltyPoints(dto.getLoyaltyPoints() != null ? dto.getLoyaltyPoints() : 0);
                entity.setIsExportCustomer(Boolean.TRUE.equals(dto.getIsExportCustomer()));
            }
            case "SUPPLIER" -> {
                entity.setSupplierCode(dto.getSupplierCode());
                entity.setLeadTimeDays(dto.getLeadTimeDays());
                entity.setPreferredCurrency(dto.getPreferredCurrency());
                entity.setIsImportSupplier(Boolean.TRUE.equals(dto.getIsImportSupplier()));
            }
            case "LC" -> {
                entity.setLcNumber(dto.getLcNumber());
                entity.setManualLcNumber(dto.getManualLcNumber());
                entity.setLcType(dto.getLcType());
                entity.setLcStatus(dto.getLcStatus() != null ? dto.getLcStatus() : "OPEN");
                entity.setTransactionCurrency(dto.getTransactionCurrency());
                entity.setLcAmount(dto.getLcAmount());
                entity.setExchangeRate(dto.getExchangeRate());
                entity.setIssueDate(dto.getIssueDate());
                entity.setExpiryDate(dto.getExpiryDate());
                entity.setShipmentDate(dto.getShipmentDate());
                entity.setReceivingDate(dto.getReceivingDate());
                entity.setTenureDays(dto.getTenureDays());
                entity.setMasterLcNo(dto.getMasterLcNo());
                entity.setBtbLcNo(dto.getBtbLcNo());
                entity.setMasterLcDate(dto.getMasterLcDate());
                entity.setPaymentTerm(dto.getPaymentTerm());
                entity.setShipmentMode(dto.getShipmentMode());
                entity.setPartialShipmentAllowed(dto.getPartialShipmentAllowed());
                entity.setBtmaCertificateRequired(dto.getBtmaCertificateRequired());
            }
            // EMPLOYEE, GENERAL, INTER_COMPANY — no extra type-specific fields
        }
        return entity;
    }

    private ChartOfAccountSub instantiate(String type) {
        return switch (type) {
            case "BANK"          -> new BankAccount();
            case "CASH"          -> new CashAccount();
            case "CUSTOMER"      -> new CustomerAccount();
            case "SUPPLIER"      -> new SupplierAccount();
            case "EMPLOYEE"      -> new EmployeeSubAccount();
            case "LC"            -> new LetterOfCredit();
            case "INTER_COMPANY" -> new InterCompanyAccount();
            default              -> new GeneralSubAccount();
        };
    }

    private ChartOfAccountSub findEntityById(Long id) {
        return subRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub-account #" + id + " not found."));
    }

    /** Maps sub-account type to the JS function prefix used in DataTable action buttons. */
    private String fnPrefix(String type) {
        if (type == null || type.isBlank()) return "sub";
        return switch (type.toUpperCase()) {
            case "BANK"          -> "bank";
            case "CASH"          -> "cash";
            case "CUSTOMER"      -> "cust";
            case "SUPPLIER"      -> "supp";
            case "EMPLOYEE"      -> "emp";
            case "LC"            -> "lc";
            case "INTER_COMPANY" -> "ic";
            default              -> "sub";
        };
    }
}
