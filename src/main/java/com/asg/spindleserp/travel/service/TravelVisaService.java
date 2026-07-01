package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvVisaApplicationDTO;
import com.asg.spindleserp.travel.dto.TrvVisaTypeDTO;

import java.util.List;
import java.util.Map;

public interface TravelVisaService {

    // ── Visa Types (lookup) ──────────────────────────────────────────────────
    TrvVisaTypeDTO saveVisaType(TrvVisaTypeDTO dto);
    void deleteVisaType(Long id);
    List<Map<String, Object>> listVisaTypes();

    // ── Visa Applications (+ document checklist) ─────────────────────────────
    TrvVisaApplicationDTO saveApplication(TrvVisaApplicationDTO dto);
    TrvVisaApplicationDTO findApplicationById(Long id);
    void deleteApplication(Long id);
    TrvVisaApplicationDTO changeApplicationStatus(Long id, String status);
    List<Map<String, Object>> listApplications(String search);
}
