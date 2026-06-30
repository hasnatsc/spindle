// Path: com/asg/spindleserp/ecommerce/service/EcCartServiceImpl.java
package com.asg.spindleserp.ecommerce.cart.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;

import com.asg.spindleserp.ecommerce.cart.dto.EcCartDTO;
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;
import com.asg.spindleserp.ecommerce.cart.repository.EcCartRepository;
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
public class EcCartServiceImpl implements EcCartService {

    private final EcCartRepository cartRepository;
    private final JdbcTemplate     jdbcTemplate;

    @Override @Transactional(readOnly = true)
    public EcCartDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    public EcCartDTO markAbandoned(Long id) {
        EcCart e = findEntityById(id);
        if (e.getCartStatus() != EcCart.CartStatus.ACTIVE)
            throw new IllegalStateException("Only ACTIVE carts can be marked abandoned.");
        e.setCartStatus(EcCart.CartStatus.ABANDONED);
        return toDTO(cartRepository.save(e));
    }

    @Override
    public void delete(Long id) {
        EcCart e = findEntityById(id);
        if (e.getCartStatus() == EcCart.CartStatus.ORDERED)
            throw new IllegalStateException("Cannot delete an ORDERED cart.");
        cartRepository.delete(e);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE ca.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "c.full_name", "c.phone", "ca.cart_status", "ca.session_id"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY ca.id DESC) AS sl,
                COUNT(*)     OVER ()                    AS full_count,
                ca.id,
                COALESCE(c.full_name, 'Guest')          AS customer_name,
                COALESCE(c.phone, '—')                  AS customer_phone,
                COALESCE(ca.session_id, '—')            AS session_id,
                ca.cart_status,
                ca.total_items,
                TO_CHAR(ca.grand_total, 'FM৳ 999,999,999.00') AS grand_total,
                TO_CHAR(ca.expires_at, 'DD-Mon-YYYY HH24:MI') AS expires_at,
                TO_CHAR(ca.created_at, 'DD-Mon-YYYY HH24:MI') AS created_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="eccartShow('    || ca.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                    || CASE WHEN ca.cart_status = 'ACTIVE'
                        THEN '<a href="javascript:;" onclick="eccartAbandon(' || ca.id || ')" class="btn btn-white btn-sm" title="Mark Abandoned"><i class="fas fa-ban text-warning"></i></a>'
                        ELSE '' END
                    || '<a href="javascript:;" onclick="eccartDelete('  || ca.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                          AS actions
            FROM ec_cart ca
            LEFT JOIN ec_customers c ON c.id = ca.customer_id
            %s ORDER BY ca.id DESC OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcCartDTO toDTO(EcCart e) {
        return EcCartDTO.builder()
                .id(e.getId())
                .customerId(e.getCustomer() != null ? e.getCustomer().getId() : null)
                .customerName(e.getCustomer() != null ? e.getCustomer().getFullName() : null)
                .customerPhone(e.getCustomer() != null ? e.getCustomer().getPhone() : null)
                .sessionId(e.getSessionId())
                .cartStatus(e.getCartStatus() != null ? e.getCartStatus().name() : null)
                .totalItems(e.getTotalItems())
                .subtotal(e.getSubtotal())
                .discountAmount(e.getDiscountAmount())
                .couponDiscount(e.getCouponDiscount())
                .shippingCharge(e.getShippingCharge())
                .taxAmount(e.getTaxAmount())
                .grandTotal(e.getGrandTotal())
                .expiresAt(e.getExpiresAt() != null ? e.getExpiresAt().toString() : null)
                .active(e.isActive())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private EcCart findEntityById(Long id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cart #" + id + " not found."));
    }
}
