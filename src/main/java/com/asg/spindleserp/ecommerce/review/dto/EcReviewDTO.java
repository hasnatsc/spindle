// Path: com/asg/spindleserp/ecommerce/dto/EcReviewDTO.java
package com.asg.spindleserp.ecommerce.review.dto;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReviewDTO {
    private Long    id;
    private Long    productId;
    private String  productTitle;
    private Long    customerId;
    private String  customerName;
    private Long    orderItemId;

    private Integer rating;          // 1–5
    private String  reviewTitle;
    private String  reviewText;
    private String  pros;
    private String  cons;
    private Boolean verifiedPurchase;
    private Boolean recommendation;

    private Integer likesCount;
    private Integer dislikesCount;
    private Integer helpfulCount;
    private Integer reportCount;

    private String  reviewStatus;    // PENDING / APPROVED / REJECTED / HIDDEN
    private String  approvedBy;
    private String  approvedAt;

    private Boolean active;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();  // simplified for display

    private String  createdAt;
    private String  updatedAt;
    private String  createdBy;
}
