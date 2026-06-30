// Path: com/asg/spindleserp/ecommerce/dto/EcProductCatalogDTO.java
package com.asg.spindleserp.ecommerce.productSupport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcProductCatalogDTO {

    private Long id;

    // ── ERP link ──────────────────────────────────────────────────────────
    @NotNull(message = "Item is required.")
    private Long   itemId;
    private String itemCode;        // denorm display
    private String itemName;        // denorm display
    private String itemSku;         // denorm display
    private String itemUom;         // denorm display
    private BigDecimal itemUnitPrice; // denorm display (ERP base price)

    // ── Storefront category ───────────────────────────────────────────────
    private Long   categoryId;
    private String categoryName;    // denorm display

    // ── Storefront identity ───────────────────────────────────────────────
    @NotBlank(message = "Slug is required.")
    @Size(max = 250)
    private String slug;

    @NotBlank(message = "Product title is required.")
    @Size(max = 300)
    private String productTitle;

    // ── Descriptions ──────────────────────────────────────────────────────
    @Size(max = 1000)
    private String shortDescription;
    private String description;

    // ── SEO ───────────────────────────────────────────────────────────────
    @Size(max = 255)
    private String seoTitle;
    @Size(max = 500)
    private String seoKeywords;
    @Size(max = 1000)
    private String seoDescription;

    // ── CMS content ───────────────────────────────────────────────────────
    @Size(max = 500)
    private String youtubeVideo;
    @Size(max = 500)
    private String warrantyInformation;
    private String returnPolicy;
    private String shippingInformation;

    // ── Order quantity overrides ──────────────────────────────────────────
    private BigDecimal minimumOrderQty;
    private BigDecimal maximumOrderQty;

    // ── Merchandising flags ───────────────────────────────────────────────
    private Boolean featured;
    private Boolean bestSeller;
    private Boolean trending;
    private Boolean newArrival;
    private Boolean recommended;

    // ── Publishing ────────────────────────────────────────────────────────
    private Boolean       published;
    private LocalDateTime publishDate;

    // ── Status ────────────────────────────────────────────────────────────
    private Boolean active;
    private Boolean deleted;

    // ── Child collections ─────────────────────────────────────────────────
    @Builder.Default
    private List<ImageDTO>    images    = new ArrayList<>();
    @Builder.Default
    private List<VariantDTO>  variants  = new ArrayList<>();
    @Builder.Default
    private List<AttrValueDTO> attrValues = new ArrayList<>();

    // ── Audit (read-only) ─────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;

    // ─────────────────────────────────────────────────────────────────────
    // Nested DTOs (avoid extra files for simple child data)
    // ─────────────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ImageDTO {
        private Long    id;
        private String  imageUrl;
        private String  thumbnailUrl;
        private String  altText;
        private Integer displayOrder;
        private Boolean isPrimary;
        private Boolean active;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VariantDTO {
        private Long       id;
        private Long       itemId;
        private String     itemCode;   // denorm
        private String     itemName;   // denorm
        private String     variantCode;
        private String     variantName;
        private String     color;
        private String     sizeName;
        private BigDecimal weightOverride;
        private BigDecimal lengthOverride;
        private BigDecimal widthOverride;
        private BigDecimal heightOverride;
        private BigDecimal sellingPrice;
        private BigDecimal comparePrice;
        private Boolean    active;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttrValueDTO {
        private Long   id;
        private Long   categoryAttributeId;
        private String attributeName;   // denorm
        private String attributeLabel;  // denorm
        private String dataType;        // denorm
        private String attributeValue;
    }
}
