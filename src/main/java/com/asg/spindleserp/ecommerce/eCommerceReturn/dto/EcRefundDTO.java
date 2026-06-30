// Path: com/asg/spindleserp/ecommerce/dto/EcRefundDTO.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcRefundDTO {
    private Long          id;
    private Long          returnId;
    private String        returnNo;
    private String        refundNo;
    private LocalDateTime refundDate;
    private String        refundMethod;   // BKASH / NAGAD / ROCKET / CARD / BANK / WALLET / CASH
    private BigDecimal    refundAmount;
    private String        transactionReference;
    private String        remarks;
    private Long          journalEntryId;
    private String        journalEntryNo;
    private String        createdAt;
    private String        updatedAt;
    private String        createdBy;
}
