package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvGlAccountDefaultsDTO;

import java.util.List;
import java.util.Map;

public interface TravelSettingsService {

    /** Returns the current org's GL defaults, or an empty (unsaved) DTO if none exist yet. */
    TrvGlAccountDefaultsDTO getDefaults();

    TrvGlAccountDefaultsDTO saveDefaults(TrvGlAccountDefaultsDTO dto);

    // ── Payment Mode → Account mapping ─────────────────────────────────────────

    /** Returns all payment-mode-to-account mappings for the current org. */
    List<Map<String, Object>> getPaymentModeAccounts();

    /** Upserts the default sub-account for a given payment mode. Pass null to unset. */
    void savePaymentModeAccount(String paymentMode, Long subAccountId);
}
