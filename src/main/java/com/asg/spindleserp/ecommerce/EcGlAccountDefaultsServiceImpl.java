// Path: com/asg/spindleserp/ecommerce/service/EcGlAccountDefaultsServiceImpl.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j @Service @Transactional @RequiredArgsConstructor
public class EcGlAccountDefaultsServiceImpl implements EcGlAccountDefaultsService {

    private final EcGlAccountDefaultsRepository glDefaultsRepository;
    private final ChartOfAccountRepository       coaRepo;
    private final ChartOfAccountSubRepository    subRepo;

    @Override @Transactional(readOnly = true)
    public EcGlAccountDefaultsDTO findOrCreateByOrg(Long orgId) {
        EcGlAccountDefaults entity = glDefaultsRepository.findByOrganizationId(orgId)
                .orElseGet(() -> {
                    EcGlAccountDefaults blank = new EcGlAccountDefaults();
                    return glDefaultsRepository.save(blank);
                });
        return toDTO(entity);
    }

    @Override
    public EcGlAccountDefaultsDTO save(EcGlAccountDefaultsDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcGlAccountDefaults entity = glDefaultsRepository.findByOrganizationId(orgId)
                .orElseGet(EcGlAccountDefaults::new);

        entity.setSalesRevenueAccount(resolveCoA(dto.getSalesRevenueAccountId()));
        entity.setSalesReturnsAccount(resolveCoA(dto.getSalesReturnsAccountId()));
        entity.setCogsAccount(resolveCoA(dto.getCogsAccountId()));
        entity.setAccountsReceivable(resolveCoA(dto.getAccountsReceivableId()));
        entity.setVatPayableAccount(resolveCoA(dto.getVatPayableAccountId()));
        entity.setDiscountExpenseAccount(resolveCoA(dto.getDiscountExpenseAccountId()));
        entity.setShippingIncomeAccount(resolveCoA(dto.getShippingIncomeAccountId()));
        entity.setDefaultBankSubAccount(resolveCoASub(dto.getDefaultBankSubAccountId()));

        return toDTO(glDefaultsRepository.save(entity));
    }

    @Override
    public EcGlAccountDefaultsDTO toDTO(EcGlAccountDefaults e) {
        return EcGlAccountDefaultsDTO.builder()
                .id(e.getId())
                .salesRevenueAccountId(e.getSalesRevenueAccount() != null ? e.getSalesRevenueAccount().getId() : null)
                .salesRevenueAccountName(e.getSalesRevenueAccount() != null ? e.getSalesRevenueAccount().getAccountName() : null)
                .salesReturnsAccountId(e.getSalesReturnsAccount() != null ? e.getSalesReturnsAccount().getId() : null)
                .salesReturnsAccountName(e.getSalesReturnsAccount() != null ? e.getSalesReturnsAccount().getAccountName() : null)
                .cogsAccountId(e.getCogsAccount() != null ? e.getCogsAccount().getId() : null)
                .cogsAccountName(e.getCogsAccount() != null ? e.getCogsAccount().getAccountName() : null)
                .accountsReceivableId(e.getAccountsReceivable() != null ? e.getAccountsReceivable().getId() : null)
                .accountsReceivableName(e.getAccountsReceivable() != null ? e.getAccountsReceivable().getAccountName() : null)
                .vatPayableAccountId(e.getVatPayableAccount() != null ? e.getVatPayableAccount().getId() : null)
                .vatPayableAccountName(e.getVatPayableAccount() != null ? e.getVatPayableAccount().getAccountName() : null)
                .discountExpenseAccountId(e.getDiscountExpenseAccount() != null ? e.getDiscountExpenseAccount().getId() : null)
                .discountExpenseAccountName(e.getDiscountExpenseAccount() != null ? e.getDiscountExpenseAccount().getAccountName() : null)
                .shippingIncomeAccountId(e.getShippingIncomeAccount() != null ? e.getShippingIncomeAccount().getId() : null)
                .shippingIncomeAccountName(e.getShippingIncomeAccount() != null ? e.getShippingIncomeAccount().getAccountName() : null)
                .defaultBankSubAccountId(e.getDefaultBankSubAccount() != null ? e.getDefaultBankSubAccount().getId() : null)
                .defaultBankSubAccountName(e.getDefaultBankSubAccount() != null ? e.getDefaultBankSubAccount().getSubAccountName() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private ChartOfAccount    resolveCoA(Long id)    { return id == null ? null : coaRepo.findById(id).orElse(null); }
    private ChartOfAccountSub resolveCoASub(Long id) { return id == null ? null : subRepo.findById(id).orElse(null); }
}
