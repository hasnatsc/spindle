// Path: com/asg/spindleserp/ecommerce/service/EcPaymentServiceImpl.java
package com.asg.spindleserp.ecommerce.payment.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.dto.EcPaymentDTO;
import com.asg.spindleserp.ecommerce.payment.EcPayment;
import com.asg.spindleserp.ecommerce.repository.EcPaymentRepository;
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
public class EcPaymentServiceImpl implements EcPaymentService {

    private final EcPaymentRepository paymentRepository;
    private final JdbcTemplate        jdbcTemplate;

    @Override @Transactional(readOnly = true)
    public EcPaymentDTO findById(Long id) { return toDTO(findEntityById(id)); }

    @Override
    public EcPaymentDTO updateStatus(Long id, String newStatus, String remarks) {
        EcPayment entity = findEntityById(id);
        EcPayment.PaymentStatus ps;
        try { ps = EcPayment.PaymentStatus.valueOf(newStatus.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid payment status: " + newStatus); }
        entity.setPaymentStatus(ps);
        if (remarks != null && !remarks.isBlank()) entity.setRemarks(remarks.trim());
        return toDTO(paymentRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        EcPayment e = findEntityById(id);
        if (e.getPaymentStatus() == EcPayment.PaymentStatus.SUCCESS)
            throw new IllegalStateException("Cannot delete a SUCCESS payment. Reverse it via a refund.");
        paymentRepository.delete(e);
    }

    @Override @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        String where = "WHERE p.organization_id = " + ContextProvider.getOrganizationId()
                + CommonUtils.searchILike(search, Arrays.asList(
                        "p.payment_no", "o.order_no", "c.full_name", "p.payment_method",
                        "p.payment_status", "p.transaction_reference"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY p.id DESC) AS sl,
                COUNT(*)     OVER ()                   AS full_count,
                p.id,
                p.payment_no,
                o.order_no,
                COALESCE(c.full_name, 'Guest')         AS customer_name,
                p.payment_method,
                p.payment_status,
                TO_CHAR(p.paid_amount, 'FM৳ 999,999,999.00') AS paid_amount,
                COALESCE(p.transaction_reference, '—') AS transaction_reference,
                COALESCE(sa.sub_account_name, '—')     AS receiving_account,
                TO_CHAR(p.payment_date, 'DD-Mon-YYYY HH24:MI') AS payment_date,
                TO_CHAR(p.created_at,  'DD-Mon-YYYY')  AS created_at,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="ecpayShow('    || p.id || ')" class="btn btn-white btn-sm"><i class="fas fa-eye text-success"></i></a>'
                    || '<a href="javascript:;" onclick="ecpayStatus('  || p.id || ')" class="btn btn-white btn-sm" title="Update"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                    || '<a href="javascript:;" onclick="ecpayDelete('  || p.id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                    || '</div>'                         AS actions
            FROM ec_payments p
            JOIN ec_orders o  ON o.id  = p.order_id
            LEFT JOIN ec_customers c  ON c.id  = o.customer_id
            LEFT JOIN acc_chart_of_accounts_sub sa ON sa.id = p.receiving_sub_account_id
            %s ORDER BY p.id DESC OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    @Override
    public EcPaymentDTO toDTO(EcPayment e) {
        return EcPaymentDTO.builder()
                .id(e.getId())
                .paymentNo(e.getPaymentNo())
                .orderId(e.getOrder() != null ? e.getOrder().getId() : null)
                .orderNo(e.getOrder() != null ? e.getOrder().getOrderNo() : null)
                .customerName(e.getOrder() != null && e.getOrder().getCustomer() != null
                        ? e.getOrder().getCustomer().getFullName() : "Guest")
                .paymentMethod(e.getPaymentMethod() != null ? e.getPaymentMethod().name() : null)
                .paymentStatus(e.getPaymentStatus() != null ? e.getPaymentStatus().name() : null)
                .paymentDate(e.getPaymentDate())
                .transactionReference(e.getTransactionReference())
                .gatewayTransactionId(e.getGatewayTransactionId())
                .currency(e.getCurrency())
                .exchangeRate(e.getExchangeRate())
                .paidAmount(e.getPaidAmount())
                .gatewayFee(e.getGatewayFee())
                .remarks(e.getRemarks())
                .journalEntryId(e.getJournalEntry() != null ? e.getJournalEntry().getId() : null)
                .journalEntryNo(e.getJournalEntry() != null ? e.getJournalEntry().getVoucherNo() : null)
                .receivingSubAccountId(e.getReceivingSubAccount() != null ? e.getReceivingSubAccount().getId() : null)
                .receivingSubAccountName(e.getReceivingSubAccount() != null ? e.getReceivingSubAccount().getSubAccountName() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .build();
    }

    private EcPayment findEntityById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment #" + id + " not found."));
    }
}
