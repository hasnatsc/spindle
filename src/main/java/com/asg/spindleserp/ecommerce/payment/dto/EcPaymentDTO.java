// Path: com/asg/spindleserp/ecommerce/dto/EcPaymentDTO.java
package com.asg.spindleserp.ecommerce.payment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcPaymentDTO {
    private Long          id;
    private String        paymentNo;

    // Order link
    private Long          orderId;
    private String        orderNo;
    private String        customerName;

    private String        paymentMethod;   // BKASH / NAGAD / SSLCOMMERZ / COD …
    private String        paymentStatus;   // PENDING / SUCCESS / FAILED / CANCELLED / REFUNDED / PARTIAL
    private LocalDateTime paymentDate;
    private String        transactionReference;
    private String        gatewayTransactionId;
    private String        currency;
    private BigDecimal    exchangeRate;
    private BigDecimal    paidAmount;
    private BigDecimal    gatewayFee;
    private String        remarks;

    // GL
    private Long          journalEntryId;
    private String        journalEntryNo;

    // Receiving sub-account
    private Long          receivingSubAccountId;
    private String        receivingSubAccountName;

    private String        createdAt;
    private String        updatedAt;
    private String        createdBy;
}
