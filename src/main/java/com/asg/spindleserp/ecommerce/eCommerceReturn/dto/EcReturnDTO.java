// Path: com/asg/spindleserp/ecommerce/dto/EcReturnDTO.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReturnDTO {
    private Long   id;
    private String returnNo;

    // Parent Order
    private Long   orderId;
    private String orderNo;

    // Customer
    private Long   customerId;
    private String customerName;
    private String customerPhone;

    private LocalDateTime returnDate;
    private String        returnReason;
    private String        returnStatus;  // REQUESTED / APPROVED / REJECTED / RECEIVED / REFUNDED / COMPLETED
    private BigDecimal    refundAmount;
    private String        remarks;

    // GL link
    private Long   journalEntryId;
    private String journalEntryNo;

    private Boolean active;

    @Builder.Default
    private List<ReturnItemDTO> returnItems = new ArrayList<>();

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReturnItemDTO {
        private Long       id;
        private Long       orderItemId;
        private String     productTitle;
        private String     itemCode;
        private String     variantName;
        private BigDecimal quantity;
        private BigDecimal approvedQty;
        private String     reason;
        private String     conditionStatus; // GOOD / DAMAGED / USED / DEFECTIVE
    }
}
