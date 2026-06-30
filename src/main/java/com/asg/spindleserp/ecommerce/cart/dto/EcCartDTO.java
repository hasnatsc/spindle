// Path: com/asg/spindleserp/ecommerce/dto/EcCartDTO.java
package com.asg.spindleserp.ecommerce.cart.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCartDTO {
    private Long       id;
    private Long       customerId;
    private String     customerName;
    private String     customerPhone;
    private String     sessionId;
    private String     cartStatus;   // ACTIVE / ORDERED / ABANDONED / EXPIRED
    private Integer    totalItems;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal couponDiscount;
    private BigDecimal shippingCharge;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private String     expiresAt;
    private Boolean    active;
    private String     createdAt;
    private String     updatedAt;
}
