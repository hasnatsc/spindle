// Path: com/asg/spindleserp/ecommerce/service/EcSettingService.java
package com.asg.spindleserp.ecommerce.settings.service;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.settings.dto.EcSettingDTO;
import com.asg.spindleserp.ecommerce.settings.entity.EcSetting;

import java.util.List;
import java.util.Map;
public interface EcSettingService {
    EcSettingDTO createOrUpdate(EcSettingDTO dto);
    EcSettingDTO findById(Long id);
    List<EcSettingDTO> findByGroup(String group);
    void bulkSave(List<EcSettingDTO> settings);
    void delete(Long id);
    Map<String, List<EcSettingDTO>> findAllGrouped();
    DataTableResponse datatableList(int draw, int start, int length, String search);
    EcSettingDTO toDTO(EcSetting entity);
}
