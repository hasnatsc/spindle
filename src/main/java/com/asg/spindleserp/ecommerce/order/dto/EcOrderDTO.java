// Path: com/asg/spindleserp/ecommerce/dto/EcOrderDTO.java
package com.asg.spindleserp.ecommerce.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcOrderDTO {
    private Long   id;
    private String orderNo;

    // Customer
    private Long   customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Status
    private String orderStatus;
    private String paymentStatus;
    private String shippingStatus;
    private String orderSource;

    // Amounts
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal couponDiscount;
    private BigDecimal shippingCharge;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;

    // Coupon
    private Long   couponId;
    private String couponCode;

    // Dates
    private LocalDateTime orderDate;
    private LocalDate     expectedDeliveryDate;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    // Notes
    private String customerNote;
    private String adminNote;

    // GL
    private Long   journalEntryId;
    private String journalEntryNo;

    private Boolean active;

    // Items (read-only on show)
    @Builder.Default
    private List<ItemDTO> orderItems = new ArrayList<>();

    // Audit
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemDTO {
        private Long       id;
        private Long       productId;
        private String     productTitle;
        private Long       itemId;
        private String     itemCode;
        private String     itemName;
        private Long       variantId;
        private String     variantName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal costPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
        private BigDecimal profitAmount;
        private String     remarks;
    }
}
