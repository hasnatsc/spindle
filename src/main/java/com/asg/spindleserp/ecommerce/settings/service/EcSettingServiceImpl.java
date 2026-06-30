// Path: com/asg/spindleserp/ecommerce/service/EcSettingServiceImpl.java
package com.asg.spindleserp.ecommerce.settings.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.settings.dto.EcSettingDTO;
import com.asg.spindleserp.ecommerce.settings.entity.EcSetting;
import com.asg.spindleserp.ecommerce.settings.repository.EcSettingRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j @Service @Transactional @RequiredArgsConstructor
public class EcSettingServiceImpl implements EcSettingService {

    private final EcSettingRepository settingRepository;
    private final JdbcTemplate        jdbcTemplate;

    @Override
    public EcSettingDTO createOrUpdate(EcSettingDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String group = dto.getSettingGroup().trim();
        String key   = dto.getSettingKey().trim();

        EcSetting entity = settingRepository
                .findByOrganizationIdAndSettingGroupAndSettingKey(orgId, group, key)
                .orElseGet(EcSetting::new);

        entity.setSettingGroup(group);
        entity.setSettingKey(key);
        entity.setSettingValue(dto.getSettingValue());
        entity.setDataType(parseDataType(dto.getDataType()));
        entity.setDescription(dto.getDescription());
        entity.setEditable(dto.getEditable() == null || dto.getEditable());
        return toDTO(settingRepository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcSettingDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcSettingDTO> findByGroup(String group) {
        Long orgId = ContextProvider.getOrganizationId();
        return settingRepository.findByOrganizationIdAndSettingGroupOrderBySettingKeyAsc(orgId, group)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public void bulkSave(List<EcSettingDTO> settings) {
        if (settings == null || settings.isEmpty()) return;
        settings.forEach(this::createOrUpdate);
    }

    @Override
    public void delete(Long id) {
        EcSetting e = findEntityById(id);
        if (!e.isEditable()) throw new IllegalStateException("This setting is system-locked and cannot be deleted.");
        settingRepository.delete(e);
    }

    @Override @Transactional(readOnly = true)
    public Map<String, List<EcSettingDTO>> findAllGrouped() {
        Long orgId = ContextProvider.getOrganizationId();
        return settingRepository.findByOrganizationIdOrderBySettingGroupAscSettingKeyAsc(orgId)
                .stream().map(this::toDTO)
                .collect(Collectors.groupingBy(EcSettingDTO::getSettingGroup, LinkedHashMap::new, Collectors.toList()));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE s.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "s.setting_group", "s.setting_key", "s.setting_value"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY s.setting_group, s.setting_key) AS sl,
                COUNT(*)     OVER ()                                         AS full_count,
                s.id,
                s.setting_group,
                s.setting_key,
                COALESCE(SUBSTRING(s.setting_value FROM 1 FOR 80), '—')     AS setting_value,
                s.data_type,
                CASE WHEN s.editable
                    THEN '<span class="badge bg-success">Editable</span>'
                    ELSE '<span class="badge bg-secondary">Locked</span>'
                END AS editable_badge,
                TO_CHAR(s.updated_at, 'DD-Mon-YYYY HH24:MI') AS updated_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ecstShow('   || s.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                    || CASE WHEN s.editable
                        THEN '<a href="javascript:;" onclick="ecstEdit(' || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                             || '<a href="javascript:;" onclick="ecstDelete(' || s.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                        ELSE '' END
                    || '</div>'                                              AS actions
            FROM ec_settings s
            %s ORDER BY s.setting_group, s.setting_key
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcSettingDTO toDTO(EcSetting e) {
        return EcSettingDTO.builder()
                .id(e.getId())
                .settingGroup(e.getSettingGroup())
                .settingKey(e.getSettingKey())
                .settingValue(e.getSettingValue())
                .dataType(e.getDataType() != null ? e.getDataType().name() : null)
                .description(e.getDescription())
                .editable(e.isEditable())
                .build();
    }

    private EcSetting findEntityById(Long id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Setting #" + id + " not found."));
    }
    private EcSetting.DataType parseDataType(String v) {
        if (v == null || v.isBlank()) return EcSetting.DataType.STRING;
        try { return EcSetting.DataType.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return EcSetting.DataType.STRING; }
    }
}
