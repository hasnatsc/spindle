package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvGlAccountDefaultsDTO;

public interface TravelSettingsService {

    /** Returns the current org's GL defaults, or an empty (unsaved) DTO if none exist yet. */
    TrvGlAccountDefaultsDTO getDefaults();

    TrvGlAccountDefaultsDTO saveDefaults(TrvGlAccountDefaultsDTO dto);
}
