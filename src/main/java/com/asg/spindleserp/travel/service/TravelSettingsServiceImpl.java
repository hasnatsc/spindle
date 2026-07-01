package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvGlAccountDefaultsDTO;
import com.asg.spindleserp.travel.entity.TrvGlAccountDefaults;
import com.asg.spindleserp.travel.repository.TrvGlAccountDefaultsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelSettingsServiceImpl implements TravelSettingsService {

    private final TrvGlAccountDefaultsRepository repo;
    private final ChartOfAccountRepository       coaRepo;

    @Override
    @Transactional(readOnly = true)
    public TrvGlAccountDefaultsDTO getDefaults() {
        Long orgId = SecurityHelper.requireOrgId();
        return repo.findByOrganizationId(orgId)
            .map(this::toDTO)
            .orElse(TrvGlAccountDefaultsDTO.builder().organizationId(orgId).build());
    }

    @Override
    public TrvGlAccountDefaultsDTO saveDefaults(TrvGlAccountDefaultsDTO dto) {
        Long orgId = SecurityHelper.requireOrgId();
        TrvGlAccountDefaults e = repo.findByOrganizationId(orgId)
            .orElse(TrvGlAccountDefaults.builder().build());
        e.setAccountsReceivableId(dto.getAccountsReceivableId());
        e.setTravelRevenueAccountId(dto.getTravelRevenueAccountId());
        e.setCostOfServiceAccountId(dto.getCostOfServiceAccountId());
        e.setSupplierPayableDefaultId(dto.getSupplierPayableDefaultId());
        return toDTO(repo.save(e));
    }

    private TrvGlAccountDefaultsDTO toDTO(TrvGlAccountDefaults e) {
        TrvGlAccountDefaultsDTO dto = TrvGlAccountDefaultsDTO.builder()
            .id(e.getId())
            .accountsReceivableId(e.getAccountsReceivableId())
            .travelRevenueAccountId(e.getTravelRevenueAccountId())
            .costOfServiceAccountId(e.getCostOfServiceAccountId())
            .supplierPayableDefaultId(e.getSupplierPayableDefaultId())
            .build();
        if (e.getTravelRevenueAccountId() != null)
            coaRepo.findById(e.getTravelRevenueAccountId())
                .ifPresent(a -> dto.setTravelRevenueAccountDisplay(a.getAccountCode() + " — " + a.getAccountName()));
        if (e.getAccountsReceivableId() != null)
            coaRepo.findById(e.getAccountsReceivableId())
                .ifPresent(a -> dto.setAccountsReceivableDisplay(a.getAccountCode() + " — " + a.getAccountName()));
        if (e.getCostOfServiceAccountId() != null)
            coaRepo.findById(e.getCostOfServiceAccountId())
                .ifPresent(a -> dto.setCostOfServiceAccountDisplay(a.getAccountCode() + " — " + a.getAccountName()));
        if (e.getSupplierPayableDefaultId() != null)
            coaRepo.findById(e.getSupplierPayableDefaultId())
                .ifPresent(a -> dto.setSupplierPayableDefaultDisplay(a.getAccountCode() + " — " + a.getAccountName()));
        return dto;
    }
}
