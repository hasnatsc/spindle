// Path: com/asg/spindleserp/ecommerce/service/EcReturnServiceImpl.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.eCommerceReturn.dto.EcReturnDTO;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcReturn;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcReturnItem;
import com.asg.spindleserp.ecommerce.eCommerceReturn.repository.EcReturnRepository;
import com.asg.spindleserp.ecommerce.order.entity.EcOrderItem;
import com.asg.spindleserp.ecommerce.order.repository.EcOrderRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j @Service @Transactional @RequiredArgsConstructor
public class EcReturnServiceImpl implements EcReturnService {

    private final EcReturnRepository returnRepository;
    private final EcOrderRepository orderRepository;
    private final DocumentSequenceService docSeqService;
    private final JdbcTemplate        jdbcTemplate;

    @Override
    public EcReturnDTO create(EcReturnDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        var order  = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order #" + dto.getOrderId() + " not found."));

        String returnNo = docSeqService.nextDocumentNumber(orgId, "EC-RET", String.valueOf(LocalDateTime.now().getYear()));

        EcReturn entity = EcReturn.builder()
                .order(order)
                .customer(order.getCustomer())
                .returnNo(returnNo)
                .returnDate(dto.getReturnDate() != null ? dto.getReturnDate() : LocalDateTime.now())
                .returnReason(dto.getReturnReason())
                .returnStatus(EcReturn.ReturnStatus.REQUESTED)
                .refundAmount(dto.getRefundAmount())
                .remarks(dto.getRemarks())
                .build();

        entity = returnRepository.save(entity);
        syncReturnItems(entity, dto.getReturnItems());
        return toDTO(returnRepository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcReturnDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    public EcReturnDTO updateStatus(Long id, String status, String remarks) {
        EcReturn entity = findEntityById(id);
        EcReturn.ReturnStatus rs;
        try { rs = EcReturn.ReturnStatus.valueOf(status.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid return status: " + status); }

        if (entity.getReturnStatus() == EcReturn.ReturnStatus.COMPLETED ||
            entity.getReturnStatus() == EcReturn.ReturnStatus.REJECTED)
            throw new IllegalStateException("Return " + entity.getReturnNo() + " is terminal and cannot be changed.");

        entity.setReturnStatus(rs);
        if (remarks != null && !remarks.isBlank()) entity.setRemarks(remarks.trim());
        return toDTO(returnRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        EcReturn entity = findEntityById(id);
        if (entity.getReturnStatus() != EcReturn.ReturnStatus.REQUESTED)
            throw new IllegalStateException("Only REQUESTED returns can be deleted.");
        returnRepository.delete(entity);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE r.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "r.return_no", "o.order_no", "c.full_name", "c.phone", "r.return_status"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY r.id DESC) AS sl,
                COUNT(*)     OVER ()                   AS full_count,
                r.id,
                r.return_no,
                o.order_no,
                COALESCE(c.full_name, 'Guest')         AS customer_name,
                COALESCE(c.phone, '—')                 AS customer_phone,
                r.return_status,
                COALESCE(r.refund_amount::text, '—')   AS refund_amount,
                (SELECT COUNT(*) FROM ec_return_items ri WHERE ri.return_id = r.id) AS item_count,
                TO_CHAR(r.return_date, 'DD-Mon-YYYY HH24:MI') AS return_date,
                TO_CHAR(r.created_at, 'DD-Mon-YYYY')   AS created_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ecretShow('    || r.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ecretStatus('  || r.id || ')" class="btn btn-white btn-sm" title="Update Status"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="ecretDelete('  || r.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                         AS actions
            FROM ec_returns r
            JOIN ec_orders o   ON o.id = r.order_id
            LEFT JOIN ec_customers c ON c.id = r.customer_id
            %s ORDER BY r.id DESC OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcReturnDTO toDTO(EcReturn e) {
        List<EcReturnDTO.ReturnItemDTO> items = e.getReturnItems().stream().map(ri ->
            EcReturnDTO.ReturnItemDTO.builder()
                .id(ri.getId())
                .orderItemId(ri.getOrderItem() != null ? ri.getOrderItem().getId() : null)
                .productTitle(ri.getOrderItem() != null && ri.getOrderItem().getProduct() != null
                        ? ri.getOrderItem().getProduct().getProductTitle() : null)
                .itemCode(ri.getOrderItem() != null && ri.getOrderItem().getItem() != null
                        ? ri.getOrderItem().getItem().getItemCode() : null)
                .variantName(ri.getOrderItem() != null && ri.getOrderItem().getVariant() != null
                        ? ri.getOrderItem().getVariant().getVariantName() : null)
                .quantity(ri.getQuantity())
                .approvedQty(ri.getApprovedQty())
                .reason(ri.getReason())
                .conditionStatus(ri.getConditionStatus() != null ? ri.getConditionStatus().name() : null)
                .build()
        ).toList();

        return EcReturnDTO.builder()
                .id(e.getId())
                .returnNo(e.getReturnNo())
                .orderId(e.getOrder() != null ? e.getOrder().getId() : null)
                .orderNo(e.getOrder() != null ? e.getOrder().getOrderNo() : null)
                .customerId(e.getCustomer() != null ? e.getCustomer().getId() : null)
                .customerName(e.getCustomer() != null ? e.getCustomer().getFullName() : "Guest")
                .customerPhone(e.getCustomer() != null ? e.getCustomer().getPhone() : null)
                .returnDate(e.getReturnDate())
                .returnReason(e.getReturnReason())
                .returnStatus(e.getReturnStatus() != null ? e.getReturnStatus().name() : null)
                .refundAmount(e.getRefundAmount())
                .remarks(e.getRemarks())
                .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
                .journalEntryNo(e.getJournalEntry() != null ? e.getJournalEntry().getVoucherNo() : null)
                .returnItems(items)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    // ── Sync return items (clear-and-reinsert) ────────────────────────────────
    private void syncReturnItems(EcReturn entity, List<EcReturnDTO.ReturnItemDTO> dtoList) {
        entity.getReturnItems().clear();
        if (dtoList == null || dtoList.isEmpty()) return;
        dtoList.forEach(d -> {
            if (d.getOrderItemId() == null) return;
            EcOrderItem orderItem = jdbcTemplate.queryForObject(
                "SELECT id FROM ec_order_items WHERE id = ?",
                (rs, row) -> {
                    EcOrderItem oi = new EcOrderItem();
                    oi.setId(rs.getLong("id"));
                    return oi;
                }, d.getOrderItemId()
            );
            EcReturnItem.ConditionStatus cs = null;
            if (d.getConditionStatus() != null) {
                try { cs = EcReturnItem.ConditionStatus.valueOf(d.getConditionStatus()); }
                catch (IllegalArgumentException ignored) {}
            }
            entity.getReturnItems().add(EcReturnItem.builder()
                    .ecReturn(entity)
                    .orderItem(orderItem)
                    .quantity(d.getQuantity())
                    .approvedQty(d.getApprovedQty())
                    .reason(d.getReason())
                    .conditionStatus(cs)
                    .build());
        });
    }

    private EcReturn findEntityById(Long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return #" + id + " not found."));
    }
}
