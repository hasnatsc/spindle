// Path: com/asg/spindleserp/ecommerce/service/EcShippingMethodServiceImpl.java
package com.asg.spindleserp.ecommerce.shipping.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.shipping.dto.EcShippingMethodDTO;
import com.asg.spindleserp.ecommerce.shipping.entity.EcShippingMethod;
import com.asg.spindleserp.ecommerce.shipping.repository.EcShippingMethodRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j @Service @Transactional @RequiredArgsConstructor
public class EcShippingMethodServiceImpl implements EcShippingMethodService {

    private final EcShippingMethodRepository shipMethodRepository;
    private final JdbcTemplate               jdbcTemplate;

    @Override
    public EcShippingMethodDTO create(EcShippingMethodDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = dto.getMethodCode().trim().toUpperCase();
        if (shipMethodRepository.existsByOrganizationIdAndMethodCode(orgId, code))
            throw new IllegalArgumentException("Shipping method code '" + code + "' already exists.");
        return toDTO(shipMethodRepository.save(EcShippingMethod.builder()
                .methodCode(code).methodName(dto.getMethodName().trim())
                .courierName(dto.getCourierName()).estimatedDays(dto.getEstimatedDays())
                .baseCharge(dto.getBaseCharge()).chargePerKg(dto.getChargePerKg())
                .cashOnDelivery(Boolean.TRUE.equals(dto.getCashOnDelivery()))
                .apiEnabled(Boolean.TRUE.equals(dto.getApiEnabled()))
                .active(dto.getActive() == null || dto.getActive())
                .build()));
    }

    @Override
    public EcShippingMethodDTO update(Long id, EcShippingMethodDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcShippingMethod e = findEntityById(id);
        String code = dto.getMethodCode().trim().toUpperCase();
        if (!e.getMethodCode().equals(code) &&
                shipMethodRepository.existsByOrganizationIdAndMethodCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Shipping method code '" + code + "' already exists.");
        e.setMethodCode(code); e.setMethodName(dto.getMethodName().trim());
        e.setCourierName(dto.getCourierName()); e.setEstimatedDays(dto.getEstimatedDays());
        e.setBaseCharge(dto.getBaseCharge()); e.setChargePerKg(dto.getChargePerKg());
        e.setCashOnDelivery(Boolean.TRUE.equals(dto.getCashOnDelivery()));
        e.setApiEnabled(Boolean.TRUE.equals(dto.getApiEnabled()));
        e.setActive(dto.getActive() != null ? dto.getActive() : e.isActive());
        return toDTO(shipMethodRepository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public EcShippingMethodDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcShippingMethodDTO> findActiveByOrg(Long orgId) {
        return shipMethodRepository.findByOrganizationIdAndActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override
    public void delete(Long id) { shipMethodRepository.delete(findEntityById(id)); }

    @Override
    public EcShippingMethodDTO toggleStatus(Long id) {
        EcShippingMethod e = findEntityById(id); e.setActive(!e.isActive());
        return toDTO(shipMethodRepository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE sm.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList("sm.method_code", "sm.method_name", "sm.courier_name"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY sm.id DESC) AS sl, COUNT(*) OVER () AS full_count,
                   sm.id, sm.method_code, sm.method_name, sm.courier_name,
                   sm.estimated_days,
                   COALESCE(sm.base_charge::text, '—') AS base_charge,
                   COALESCE(sm.charge_per_kg::text, '—') AS charge_per_kg,
                   CASE WHEN sm.cash_on_delivery THEN '<span class="badge bg-info">COD</span>' ELSE '—' END AS cod_badge,
                   TO_CHAR(sm.created_at, 'DD-Mon-YYYY') AS created_at,
                   CASE WHEN sm.active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-danger">Inactive</span>' END AS status,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="ecshipShow(' || sm.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="ecshipEdit(' || sm.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="ecshipToggle(' || sm.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="ecshipDelete(' || sm.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM ec_shipping_methods sm %s ORDER BY sm.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcShippingMethodDTO toDTO(EcShippingMethod e) {
        return EcShippingMethodDTO.builder()
                .id(e.getId()).methodCode(e.getMethodCode()).methodName(e.getMethodName())
                .courierName(e.getCourierName()).estimatedDays(e.getEstimatedDays())
                .baseCharge(e.getBaseCharge()).chargePerKg(e.getChargePerKg())
                .cashOnDelivery(e.isCashOnDelivery()).apiEnabled(e.isApiEnabled())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy()).build();
    }

    private EcShippingMethod findEntityById(Long id) {
        return shipMethodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipping method #" + id + " not found."));
    }
}
