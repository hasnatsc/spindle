// Path: com/asg/spindleserp/storefront/dto/SfProductCardDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;
import java.math.BigDecimal;

/** Lightweight DTO for grid/listing pages — avoids loading full product graph. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfProductCardDTO {
    private Long       id;
    private String     productTitle;
    private String     slug;
    private String     primaryImageUrl;
    private BigDecimal sellingPrice;
    private BigDecimal comparePrice;
    private Integer     discountPercent;
    private String     categoryName;
    private String     categorySlug;
    private boolean    inStock;
    private boolean    featured;
    private boolean    newArrival;
    private boolean    bestSeller;
    private Double     avgRating;
    private Integer    reviewCount;
}
