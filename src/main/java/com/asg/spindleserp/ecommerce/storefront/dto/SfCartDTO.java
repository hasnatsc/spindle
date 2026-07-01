// Path: com/asg/spindleserp/storefront/dto/SfCartDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfCartDTO {
    private Long       id;
    private Integer    totalItems;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal couponDiscount;
    private String     couponCode;
    private BigDecimal shippingCharge;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;

    @Builder.Default
    private List<SfCartItemDTO> items = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SfCartItemDTO {
        private Long       id;
        private Long       productId;
        private String     productTitle;
        private String     productSlug;
        private String     productImage;
        private Long       variantId;
        private String     variantName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal comparePrice;
        private BigDecimal lineTotal;
        private BigDecimal availableStock;
        private boolean    inStock;
    }
}
