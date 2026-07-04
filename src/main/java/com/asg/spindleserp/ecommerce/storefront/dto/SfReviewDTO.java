// Path: com/asg/spindleserp/ecommerce/storefront/dto/SfReviewDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfReviewDTO {
    private Long id;
    private Integer rating;
    private String reviewTitle;
    private String reviewText;
    private String customerName;
    private String createdAt;                                  // pre-formatted dd MMM yyyy
    @Builder.Default private Boolean verifiedPurchase = Boolean.FALSE;
}
