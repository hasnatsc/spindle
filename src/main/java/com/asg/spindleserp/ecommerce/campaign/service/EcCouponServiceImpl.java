// Path: com/asg/spindleserp/ecommerce/service/EcCouponServiceImpl.java
package com.asg.spindleserp.ecommerce.campaign.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.campaign.dto.EcCouponDTO;
import com.asg.spindleserp.ecommerce.campaign.entity.EcCoupon;
import com.asg.spindleserp.ecommerce.campaign.repository.EcCouponRepository;
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
public class EcCouponServiceImpl implements EcCouponService {

    private final EcCouponRepository couponRepository;
    private final JdbcTemplate       jdbcTemplate;

    @Override
    public EcCouponDTO create(EcCouponDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        String code = dto.getCouponCode().trim().toUpperCase();
        if (couponRepository.existsByOrganizationIdAndCouponCode(orgId, code))
            throw new IllegalArgumentException("Coupon code '" + code + "' already exists.");
        EcCoupon entity = EcCoupon.builder()
                .couponCode(code)
                .couponName(dto.getCouponName())
                .description(dto.getDescription())
                .discountType(parseDiscountType(dto.getDiscountType()))
                .discountValue(dto.getDiscountValue())
                .minimumOrder(dto.getMinimumOrder())
                .maximumDiscount(dto.getMaximumDiscount())
                .usageLimit(dto.getUsageLimit())
                .usagePerCustomer(dto.getUsagePerCustomer() != null ? dto.getUsagePerCustomer() : 1)
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .active(dto.getActive() == null || dto.getActive())
                .build();
        return toDTO(couponRepository.save(entity));
    }

    @Override
    public EcCouponDTO update(Long id, EcCouponDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcCoupon entity = findEntityById(id);
        String code = dto.getCouponCode().trim().toUpperCase();
        if (!entity.getCouponCode().equals(code) &&
                couponRepository.existsByOrganizationIdAndCouponCodeAndIdNot(orgId, code, id))
            throw new IllegalArgumentException("Coupon code '" + code + "' already exists.");
        entity.setCouponCode(code);
        entity.setCouponName(dto.getCouponName());
        entity.setDescription(dto.getDescription());
        entity.setDiscountType(parseDiscountType(dto.getDiscountType()));
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setMinimumOrder(dto.getMinimumOrder());
        entity.setMaximumDiscount(dto.getMaximumDiscount());
        entity.setUsageLimit(dto.getUsageLimit());
        entity.setUsagePerCustomer(dto.getUsagePerCustomer() != null ? dto.getUsagePerCustomer() : 1);
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidTo(dto.getValidTo());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.isActive());
        return toDTO(couponRepository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcCouponDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public List<EcCouponDTO> findActiveByOrg(Long orgId) {
        return couponRepository.findByOrganizationIdAndActiveTrue(orgId).stream().map(this::toDTO).toList();
    }

    @Override
    public void delete(Long id) { couponRepository.delete(findEntityById(id)); }

    @Override
    public EcCouponDTO toggleStatus(Long id) {
        EcCoupon e = findEntityById(id); e.setActive(!e.isActive());
        return toDTO(couponRepository.save(e));
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE c.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList("c.coupon_code", "c.coupon_name", "c.discount_type"));
        String sql = String.format("""
            SELECT ROW_NUMBER() OVER (ORDER BY c.id DESC) AS sl,
                   COUNT(*) OVER () AS full_count,
                   c.id, c.coupon_code, c.coupon_name, c.discount_type,
                   c.discount_value, c.minimum_order, c.usage_limit,
                   TO_CHAR(c.valid_from, 'DD-Mon-YYYY') AS valid_from,
                   TO_CHAR(c.valid_to,   'DD-Mon-YYYY') AS valid_to,
                   TO_CHAR(c.created_at, 'DD-Mon-YYYY') AS created_at,
                   CASE WHEN c.active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-danger">Inactive</span>' END AS status,
                   '<div class="btn-group">'
                   || '<a href="javascript:;" onclick="ecoupShow(' || c.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                   || '<a href="javascript:;" onclick="ecoupEdit(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                   || '<a href="javascript:;" onclick="ecoupToggle(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-square-check text-primary"></i></a>'
                   || '<a href="javascript:;" onclick="ecoupDelete(' || c.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                   || '</div>' AS actions
            FROM ec_coupon c %s ORDER BY c.id DESC OFFSET %d LIMIT %d
            """, where, start, length);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcCouponDTO toDTO(EcCoupon e) {
        return EcCouponDTO.builder()
                .id(e.getId()).couponCode(e.getCouponCode()).couponName(e.getCouponName())
                .description(e.getDescription())
                .discountType(e.getDiscountType() != null ? e.getDiscountType().name() : null)
                .discountValue(e.getDiscountValue()).minimumOrder(e.getMinimumOrder())
                .maximumDiscount(e.getMaximumDiscount()).usageLimit(e.getUsageLimit())
                .usagePerCustomer(e.getUsagePerCustomer()).validFrom(e.getValidFrom()).validTo(e.getValidTo())
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .build();
    }

    private EcCoupon findEntityById(Long id) {
        return couponRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Coupon #" + id + " not found."));
    }
    private EcCoupon.DiscountType parseDiscountType(String v) {
        if (v == null || v.isBlank()) return EcCoupon.DiscountType.PERCENTAGE;
        try { return EcCoupon.DiscountType.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return EcCoupon.DiscountType.PERCENTAGE; }
    }
}
