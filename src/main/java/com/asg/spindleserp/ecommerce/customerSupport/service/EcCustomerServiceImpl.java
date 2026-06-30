// Path: com/asg/spindleserp/ecommerce/service/EcCustomerServiceImpl.java
package com.asg.spindleserp.ecommerce.customerSupport.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.customerSupport.dto.EcCustomerDTO;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.customerSupport.repository.EcCustomerRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcCustomerServiceImpl implements EcCustomerService {

    private final EcCustomerRepository customerRepository;
    private final JdbcTemplate          jdbcTemplate;

    @Override
    public EcCustomerDTO create(EcCustomerDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = dto.getCustomerCode().trim().toUpperCase();

        if (customerRepository.existsByOrganizationIdAndCustomerCode(orgId, code))
            throw new IllegalArgumentException("Customer code '" + code + "' already exists.");
        if (dto.getPhone() != null && customerRepository.existsByOrganizationIdAndPhone(orgId, dto.getPhone().trim()))
            throw new IllegalArgumentException("Phone '" + dto.getPhone().trim() + "' already registered.");

        EcCustomer entity = EcCustomer.builder()
                .organizationId(orgId)
                .customerCode(code)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName())
                .fullName(buildFullName(dto))
                .email(dto.getEmail())
                .phone(dto.getPhone().trim())
                .gender(parseEnum(EcCustomer.Gender.class, dto.getGender()))
                .dateOfBirth(dto.getDateOfBirth())
                .profileImage(dto.getProfileImage())
                .companyName(dto.getCompanyName())
                .taxNumber(dto.getTaxNumber())
                .nationalId(dto.getNationalId())
                .emailVerified(Boolean.TRUE.equals(dto.getEmailVerified()))
                .phoneVerified(Boolean.TRUE.equals(dto.getPhoneVerified()))
                .accountStatus(parseEnum(EcCustomer.AccountStatus.class, dto.getAccountStatus(), EcCustomer.AccountStatus.PENDING))
                .customerGroup(dto.getCustomerGroup())
                .active(dto.getActive() == null || dto.getActive())
                .deleted(false)
                .build();
        return toDTO(customerRepository.save(entity));
    }

    @Override
    public EcCustomerDTO update(Long id, EcCustomerDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcCustomer entity = findEntityById(id);
        String code = dto.getCustomerCode().trim().toUpperCase();

        if (!entity.getCustomerCode().equals(code) &&
                customerRepository.existsByOrganizationIdAndCustomerCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Customer code '" + code + "' already exists.");
        if (dto.getPhone() != null && !entity.getPhone().equals(dto.getPhone().trim()) &&
                customerRepository.existsByOrganizationIdAndPhoneAndIdNot(orgId, dto.getPhone().trim(), id))
            throw new IllegalArgumentException("Phone '" + dto.getPhone().trim() + "' already registered.");

        entity.setCustomerCode(code);
        entity.setFirstName(dto.getFirstName().trim());
        entity.setLastName(dto.getLastName());
        entity.setFullName(buildFullName(dto));
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone().trim());
        entity.setGender(parseEnum(EcCustomer.Gender.class, dto.getGender()));
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setProfileImage(dto.getProfileImage());
        entity.setCompanyName(dto.getCompanyName());
        entity.setTaxNumber(dto.getTaxNumber());
        entity.setNationalId(dto.getNationalId());
        entity.setEmailVerified(Boolean.TRUE.equals(dto.getEmailVerified()));
        entity.setPhoneVerified(Boolean.TRUE.equals(dto.getPhoneVerified()));
        entity.setAccountStatus(parseEnum(EcCustomer.AccountStatus.class, dto.getAccountStatus(), EcCustomer.AccountStatus.PENDING));
        entity.setCustomerGroup(dto.getCustomerGroup());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(customerRepository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcCustomerDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcCustomerDTO> findActiveByOrg(Long orgId) {
        return customerRepository.findByOrganizationIdAndActiveTrueAndDeletedFalse(orgId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public void delete(Long id) {
        EcCustomer e = findEntityById(id);
        e.setDeleted(true); e.setActive(false);
        customerRepository.save(e);
    }

    @Override
    public EcCustomerDTO toggleStatus(Long id) {
        EcCustomer e = findEntityById(id);
        e.setActive(!e.isActive());
        if (!e.isActive()) e.setAccountStatus(EcCustomer.AccountStatus.BLOCKED);
        else e.setAccountStatus(EcCustomer.AccountStatus.ACTIVE);
        return toDTO(customerRepository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE c.deleted = false AND c.organization_id = "
                + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "c.customer_code", "c.full_name", "c.email", "c.phone",
                        "c.company_name", "c.customer_group"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY c.id DESC)   AS sl,
                COUNT(*)     OVER ()                     AS full_count,
                c.id,
                c.customer_code,
                c.full_name,
                COALESCE(c.email, '—')                   AS email,
                c.phone,
                COALESCE(c.company_name, '—')            AS company_name,
                COALESCE(c.customer_group, '—')          AS customer_group,
                c.total_orders,
                TO_CHAR(c.total_purchase, 'FM৳ 999,999,999.00') AS total_purchase,
                c.account_status,
                TO_CHAR(c.created_at, 'DD-Mon-YYYY')    AS created_at,
                CASE WHEN c.active
                    THEN '<span class="badge bg-success">Active</span>'
                    ELSE '<span class="badge bg-danger">Inactive</span>'
                END AS status,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="eccustShow('   || c.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="eccustEdit('   || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="eccustToggle(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                    || '<a href="javascript:;" onclick="eccustDelete(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                          AS actions
            FROM ec_customers c
            %s
            ORDER BY c.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcCustomerDTO toDTO(EcCustomer e) {
        return EcCustomerDTO.builder()
                .id(e.getId())
                .customerCode(e.getCustomerCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .gender(e.getGender() != null ? e.getGender().name() : null)
                .dateOfBirth(e.getDateOfBirth())
                .profileImage(e.getProfileImage())
                .companyName(e.getCompanyName())
                .taxNumber(e.getTaxNumber())
                .nationalId(e.getNationalId())
                .emailVerified(e.getEmailVerified())
                .phoneVerified(e.getPhoneVerified())
                .accountStatus(e.getAccountStatus() != null ? e.getAccountStatus().name() : null)
                .customerGroup(e.getCustomerGroup())
                .totalOrders(e.getTotalOrders())
                .totalPurchase(e.getTotalPurchase())
                .rewardPoints(e.getRewardPoints())
                .erpSubAccountId(e.getErpSubAccount() != null ? e.getErpSubAccount().getId() : null)
                .erpSubAccountName(e.getErpSubAccount() != null ? e.getErpSubAccount().getSubAccountName() : null)
                .active(e.isActive())
                .lastLoginAt(e.getLastLoginAt() != null ? e.getLastLoginAt().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private EcCustomer findEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer #" + id + " not found."));
    }
    private String buildFullName(EcCustomerDTO d) {
        String fn = d.getFirstName() != null ? d.getFirstName().trim() : "";
        String ln = d.getLastName()  != null ? d.getLastName().trim()  : "";
        return (fn + " " + ln).trim();
    }
    private <T extends Enum<T>> T parseEnum(Class<T> cls, String val) {
        if (val == null || val.isBlank()) return null;
        try { return Enum.valueOf(cls, val.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
    private <T extends Enum<T>> T parseEnum(Class<T> cls, String val, T def) {
        T v = parseEnum(cls, val);
        return v != null ? v : def;
    }
}
