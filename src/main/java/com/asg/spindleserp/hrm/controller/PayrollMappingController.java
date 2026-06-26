package com.asg.spindleserp.hrm.controller;

import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.hrm.entity.PayrollAccountMapping;
import com.asg.spindleserp.hrm.repository.PayrollAccountMappingRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hrm/payroll-mapping")
@RequiredArgsConstructor
public class PayrollMappingController {

    private final PayrollAccountMappingRepository mappingRepo;
    private final ChartOfAccountRepository coaRepo;

    @GetMapping
    public String page(Model m) {
        m.addAttribute("activePage", "hrm-payroll-mapping");
        return "hrm/hrm-payroll-mapping";
    }

    @GetMapping("/show")
    @ResponseBody
    public Map<String, Object> show() {
        Map<String, Object> res = new HashMap<>();
        try {
            Long orgId = SecurityHelper.currentOrgId().orElse(null);
            PayrollAccountMapping mapping = mappingRepo.findByOrganizationId(orgId)
                    .orElse(new PayrollAccountMapping());
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(mapping)));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long orgId = ContextProvider.getOrganizationId();
            String user = SecurityHelper.currentUsername().orElse("system");

            PayrollAccountMapping mapping = mappingRepo.findByOrganizationId(orgId).orElse(PayrollAccountMapping.builder().build());

            mapping.setBasicSalaryAccount(    coaRef(body, "basicSalaryAccountId"));
            mapping.setHouseRentAccount(      coaRef(body, "houseRentAccountId"));
            mapping.setMedicalAccount(        coaRef(body, "medicalAccountId"));
            mapping.setTransportAccount(      coaRef(body, "transportAccountId"));
            mapping.setOvertimeAccount(       coaRef(body, "overtimeAccountId"));
            mapping.setOtherAllowancesAccount(coaRef(body, "otherAllowancesAccountId"));
            mapping.setSalaryPayableAccount(  coaRef(body, "salaryPayableAccountId"));
            mapping.setIncomeTaxPayableAccount(coaRef(body, "incomeTaxPayableAccountId"));
            mapping.setProvidentFundPayableAccount(coaRef(body, "providentFundPayableAccountId"));
            mapping.setOtherDeductionsPayableAccount(coaRef(body, "otherDeductionsPayableAccountId"));
            mapping.setUpdatedBy(user);

            mappingRepo.save(mapping);
            res.put("success", true);
            res.put("message", "Payroll GL mapping saved successfully!");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @GetMapping("/accounts/search")
    @ResponseBody
    public Map<String, Object> searchAccounts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        int sz = 30, off = (page - 1) * sz;
        String sql = "SELECT id, account_code, account_name, account_type FROM acc_chart_of_accounts " +
                "WHERE is_active = true AND allow_manual_entry = true" +
                (orgId != null ? " AND organization_id = " + orgId : "") +
                (search != null && !search.isBlank() ?
                        " AND (account_code ILIKE '%" + search.replace("'", "''") + "%' " +
                        " OR account_name ILIKE '%" + search.replace("'", "''") + "%')" : "") +
                " ORDER BY account_code LIMIT " + (sz + 1) + " OFFSET " + off;

        // Use JdbcTemplate via coaRepo (injected separately if needed)
        List<Map<String, Object>> items = coaRepo.searchForSelect(orgId, search,  PageRequest.of(page - 1, sz)).stream()
                .limit(sz)
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("text", a.getAccountCode() + " — " + a.getAccountName());
                    m.put("type", a.getAccountType().name());
                    return m;
                }).collect(Collectors.toList());

        return Map.of("items", items, "hasMore", items.size() > sz);
    }

    private com.asg.spindleserp.accounts.entity.ChartOfAccount coaRef(
            Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) return null;
        Long id = Long.parseLong(val.toString());
        return coaRepo.getReferenceById(id);
    }

    private Map<String, Object> toMap(PayrollAccountMapping m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        putAcct(map, "basicSalaryAccount",      m.getBasicSalaryAccount());
        putAcct(map, "houseRentAccount",         m.getHouseRentAccount());
        putAcct(map, "medicalAccount",           m.getMedicalAccount());
        putAcct(map, "transportAccount",         m.getTransportAccount());
        putAcct(map, "overtimeAccount",          m.getOvertimeAccount());
        putAcct(map, "otherAllowancesAccount",   m.getOtherAllowancesAccount());
        putAcct(map, "salaryPayableAccount",     m.getSalaryPayableAccount());
        putAcct(map, "incomeTaxPayableAccount",  m.getIncomeTaxPayableAccount());
        putAcct(map, "providentFundPayableAccount", m.getProvidentFundPayableAccount());
        putAcct(map, "otherDeductionsPayableAccount", m.getOtherDeductionsPayableAccount());
        return map;
    }

    private void putAcct(Map<String, Object> map, String prefix,
                         com.asg.spindleserp.accounts.entity.ChartOfAccount acct) {
        if (acct != null) {
            map.put(prefix + "Id",   acct.getId());
            map.put(prefix + "Text", acct.getAccountCode() + " — " + acct.getAccountName());
        } else {
            map.put(prefix + "Id",   null);
            map.put(prefix + "Text", null);
        }
    }
}