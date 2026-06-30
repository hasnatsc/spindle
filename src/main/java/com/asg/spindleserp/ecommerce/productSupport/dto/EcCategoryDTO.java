// Path: com/asg/spindleserp/ecommerce/dto/EcCategoryDTO.java
package com.asg.spindleserp.ecommerce.productSupport.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCategoryDTO {
    private Long    id;
    private Long    parentCategoryId;
    private String  parentCategoryName;  // denorm

    @NotBlank @Size(max = 50)
    private String categoryCode;
    @NotBlank @Size(max = 200)
    private String categoryName;
    @NotBlank @Size(max = 250)
    private String slug;

    private String  shortDescription;
    private String  description;
    private String  imageUrl;
    private String  bannerUrl;
    private String  icon;

    // SEO
    private String  metaTitle;
    private String  metaKeywords;
    private String  metaDescription;

    // Display
    private Integer displayOrder;
    private Integer levelNo;
    private Boolean isMenu;
    private Boolean isFeatured;
    private Boolean active;

    // Child attribute schema
    @Builder.Default
    private List<AttrDTO> attributes = new ArrayList<>();

    // Audit
    private String createdAt;
    private String updatedAt;
    private String createdBy;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttrDTO {
        private Long    id;
        @NotBlank private String attributeName;
        private String  attributeLabel;
        private String  dataType;   // EcCategoryAttribute.DataType name
        private Boolean isRequired;
        private Boolean searchable;
        private Boolean filterable;
        private Boolean sortable;
        private Integer displayOrder;
        private Boolean active;
    }
}
