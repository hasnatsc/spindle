// Path: com/asg/spindleserp/ecommerce/service/EcOrderServiceImpl.java
package com.asg.spindleserp.ecommerce.order.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.order.dto.EcOrderDTO;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.entity.EcOrderStatusHistory;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EcOrderServiceImpl implements EcOrderService {

    private final EcOrderRepository orderRepository;
    private final JdbcTemplate      jdbcTemplate;

    @Override @Transactional(readOnly = true)
    public EcOrderDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    public EcOrderDTO updateStatus(Long id, String newStatus, String adminNote) {
        EcOrder entity = findEntityById(id);
        EcOrder.OrderStatus status;
        try { status = EcOrder.OrderStatus.valueOf(newStatus.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid order status: " + newStatus); }

        // Guard: COMPLETED / CANCELLED are terminal
        if (entity.getOrderStatus() == EcOrder.OrderStatus.COMPLETED ||
            entity.getOrderStatus() == EcOrder.OrderStatus.CANCELLED)
            throw new IllegalStateException("Order " + entity.getOrderNo() + " is in a terminal state and cannot be updated.");

        entity.setOrderStatus(status);
        if (adminNote != null && !adminNote.isBlank())
            entity.setAdminNote(adminNote.trim());

        // Record status history
        String changedBy = SecurityHelper.currentUsername().orElse("system");
        EcOrderStatusHistory hist = EcOrderStatusHistory.builder()
                .order(entity)
                .status(status.name())
                .remarks(adminNote)
                .changedBy(changedBy)
                .build();
        entity.getStatusHistory().add(hist);
        return toDTO(orderRepository.save(entity));
    }

    @Override
    public EcOrderDTO updateAdminNote(Long id, String note) {
        EcOrder entity = findEntityById(id);
        entity.setAdminNote(note);
        return toDTO(orderRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        EcOrder entity = findEntityById(id);
        if (entity.getOrderStatus() != EcOrder.OrderStatus.PENDING)
            throw new IllegalStateException("Only PENDING orders can be deleted.");
        orderRepository.delete(entity);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE o.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "o.order_no", "c.full_name", "c.phone", "c.email",
                        "o.order_status", "o.payment_status"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY o.id DESC)    AS sl,
                COUNT(*)     OVER ()                      AS full_count,
                o.id,
                o.order_no,
                COALESCE(c.full_name, 'Guest')            AS customer_name,
                COALESCE(c.phone, '—')                    AS customer_phone,
                TO_CHAR(o.grand_total, 'FM৳ 999,999,999.00') AS grand_total,
                o.order_status,
                o.payment_status,
                o.shipping_status,
                TO_CHAR(o.created_at, 'DD-Mon-YYYY HH24:MI') AS order_date,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ecordShow('      || o.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ecordStatus('    || o.id || ')" class="btn btn-white btn-sm" title="Update Status"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="ecordDelete('    || o.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                           AS actions
            FROM ec_orders o
            LEFT JOIN ec_customers c ON c.id = o.customer_id
            %s
            ORDER BY o.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcOrderDTO toDTO(EcOrder e) {
        List<EcOrderDTO.ItemDTO> items = e.getOrderItems().stream().map(i ->
            EcOrderDTO.ItemDTO.builder()
                .id(i.getId())
                .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                .productTitle(i.getProduct() != null ? i.getProduct().getProductTitle() : null)
                .itemId(i.getItem() != null ? i.getItem().getId() : null)
                .itemCode(i.getItem() != null ? i.getItem().getItemCode() : null)
                .itemName(i.getItem() != null ? i.getItem().getItemName() : null)
                .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                .variantName(i.getVariant() != null ? i.getVariant().getVariantName() : null)
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .costPrice(i.getCostPrice())
                .discountAmount(i.getDiscountAmount())
                .taxAmount(i.getTaxAmount())
                .lineTotal(i.getLineTotal())
                .profitAmount(i.getProfitAmount())
                .remarks(i.getRemarks())
                .build()
        ).toList();

        return EcOrderDTO.builder()
                .id(e.getId())
                .orderNo(e.getOrderNo())
                .customerId(e.getCustomer() != null ? e.getCustomer().getId() : null)
                .customerName(e.getCustomer() != null ? e.getCustomer().getFullName() : "Guest")
                .customerPhone(e.getCustomer() != null ? e.getCustomer().getPhone() : null)
                .customerEmail(e.getCustomer() != null ? e.getCustomer().getEmail() : null)
                .orderStatus(e.getOrderStatus() != null ? e.getOrderStatus().name() : null)
                .paymentStatus(e.getPaymentStatus() != null ? e.getPaymentStatus().name() : null)
                .shippingStatus(e.getShippingStatus() != null ? e.getShippingStatus().name() : null)
                .orderSource(e.getOrderSource() != null ? e.getOrderSource().name() : null)
                .subtotal(e.getSubtotal())
//                .discountAmount(e.getDiscountAmount())
                .couponDiscount(e.getCouponDiscount())
                .shippingCharge(e.getShippingCharge())
                .taxAmount(e.getTaxAmount())
                .grandTotal(e.getGrandTotal())
//                .couponId(e.getCouponId())
//                .couponCode(e.getCouponCode())
                .orderDate(e.getOrderDate())
                .expectedDeliveryDate(e.getExpectedDeliveryDate())
                .completedAt(e.getCompletedAt())
                .cancelledAt(e.getCancelledAt())
                .customerNote(e.getCustomerNote())
                .adminNote(e.getAdminNote())
                .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
                .journalEntryNo(e.getJournalEntry() != null ? e.getJournalEntry().getVoucherNo() : null)
                .active(e.isActive())
                .orderItems(items)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private EcOrder findEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order #" + id + " not found."));
    }
}
