// Path: com/asg/spindleserp/storefront/dto/SfProductDetailDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfProductDetailDTO {
    private Long       id;
    private String     productTitle;
    private String     slug;
    private String     shortDescription;
    private String     description;
    private String     itemCode;
    private String     sku;
    private String     brandName;

    private BigDecimal sellingPrice;
    private BigDecimal comparePrice;
    private Integer    discountPercent;
    private String     salesUnitCode;
    private BigDecimal availableStock;
    private boolean    inStock;
    private BigDecimal minOrderQty;
    private BigDecimal maxOrderQty;

    private Long       categoryId;
    private String     categoryName;
    private String     categorySlug;

    private String     metaTitle;
    private String     metaDescription;

    private Double     avgRating;
    private Integer    reviewCount;

    @Builder.Default private List<String> imageUrls = new ArrayList<>();
    @Builder.Default private List<VariantDTO> variants = new ArrayList<>();
    @Builder.Default private List<AttrDTO> attributes = new ArrayList<>();
    @Builder.Default private List<SfProductCardDTO> relatedProducts = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VariantDTO {
        private Long       id;
        private String     variantName;
        private String     colorCode;
        private String     sizeLabel;
        private BigDecimal sellingPrice;
        private BigDecimal availableStock;
        private boolean    inStock;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttrDTO {
        private String attributeLabel;
        private String value;
    }
}
