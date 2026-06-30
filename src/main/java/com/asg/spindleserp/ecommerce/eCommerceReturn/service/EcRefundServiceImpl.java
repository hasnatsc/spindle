// Path: com/asg/spindleserp/ecommerce/service/EcRefundServiceImpl.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.eCommerceReturn.dto.EcRefundDTO;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcRefund;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcReturn;
import com.asg.spindleserp.ecommerce.eCommerceReturn.repository.EcRefundRepository;
import com.asg.spindleserp.ecommerce.eCommerceReturn.repository.EcReturnRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
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
public class EcRefundServiceImpl implements EcRefundService {

    private final EcRefundRepository refundRepository;
    private final EcReturnRepository returnRepository;
    private final DocumentSequenceService docSeqService;
    private final JdbcTemplate        jdbcTemplate;

    @Override
    public EcRefundDTO create(EcRefundDTO dto) {
        Long orgId = ContextProvider.getOrganizationId();
        EcReturn ecReturn = returnRepository.findById(dto.getReturnId())
                .orElseThrow(() -> new IllegalArgumentException("Return #" + dto.getReturnId() + " not found."));
        if (ecReturn.getReturnStatus() != EcReturn.ReturnStatus.APPROVED &&
            ecReturn.getReturnStatus() != EcReturn.ReturnStatus.RECEIVED)
            throw new IllegalStateException("Return must be APPROVED or RECEIVED before issuing a refund.");

        String refundNo = docSeqService.nextDocumentNumber(orgId, "EC-RFND", String.valueOf(LocalDateTime.now().getYear()));

        EcRefund entity = EcRefund.builder()
                .ecReturn(ecReturn)
                .refundNo(refundNo)
                .refundDate(dto.getRefundDate() != null ? dto.getRefundDate() : LocalDateTime.now())
                .refundMethod(parseRefundMethod(dto.getRefundMethod()))
                .refundAmount(dto.getRefundAmount())
                .transactionReference(dto.getTransactionReference())
                .remarks(dto.getRemarks())
                .build();
        return toDTO(refundRepository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public EcRefundDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    public void delete(Long id) { refundRepository.delete(findEntityById(id)); }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE r.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "rf.refund_no", "r.return_no", "o.order_no", "rf.refund_method"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY rf.id DESC) AS sl,
                COUNT(*)     OVER ()                    AS full_count,
                rf.id,
                rf.refund_no,
                r.return_no,
                o.order_no,
                COALESCE(c.full_name, 'Guest')          AS customer_name,
                rf.refund_method,
                TO_CHAR(rf.refund_amount, 'FM৳ 999,999,999.00') AS refund_amount,
                COALESCE(rf.transaction_reference, '—') AS transaction_reference,
                TO_CHAR(rf.refund_date, 'DD-Mon-YYYY HH24:MI') AS refund_date,
                TO_CHAR(rf.created_at, 'DD-Mon-YYYY')   AS created_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ecrefShow('   || rf.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ecrefDelete(' || rf.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                          AS actions
            FROM ec_refunds rf
            JOIN ec_returns r  ON r.id  = rf.return_id
            JOIN ec_orders  o  ON o.id  = r.order_id
            LEFT JOIN ec_customers c ON c.id = r.customer_id
            %s ORDER BY rf.id DESC OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcRefundDTO toDTO(EcRefund e) {
        return EcRefundDTO.builder()
                .id(e.getId())
                .returnId(e.getEcReturn() != null ? e.getEcReturn().getId() : null)
                .returnNo(e.getEcReturn() != null ? e.getEcReturn().getReturnNo() : null)
                .refundNo(e.getRefundNo())
                .refundDate(e.getRefundDate())
                .refundMethod(e.getRefundMethod() != null ? e.getRefundMethod().name() : null)
                .refundAmount(e.getRefundAmount())
                .transactionReference(e.getTransactionReference())
                .remarks(e.getRemarks())
                .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
                .journalEntryNo(e.getJournalEntry() != null ? e.getJournalEntry().getVoucherNo() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .build();
    }

    private EcRefund findEntityById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund #" + id + " not found."));
    }
    private EcRefund.RefundMethod parseRefundMethod(String v) {
        if (v == null || v.isBlank()) return EcRefund.RefundMethod.CASH;
        try { return EcRefund.RefundMethod.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return EcRefund.RefundMethod.CASH; }
    }
}
