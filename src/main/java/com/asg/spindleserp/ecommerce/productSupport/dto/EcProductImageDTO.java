// Path: com/asg/spindleserp/ecommerce/productSupport/dto/EcProductImageDTO.java
package com.asg.spindleserp.ecommerce.productSupport.dto;

import lombok.*;

/**
 * EcProductImageDTO — standalone DTO for single-image upload/update/delete.
 *
 * Distinct from EcProductCatalogDTO.ImageDTO (the embedded child used in the
 * full product create/update payload). This DTO backs the dedicated image
 * endpoints so a single image can be added, edited, or removed without
 * resending the entire product graph.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcProductImageDTO {
    private Long    id;
    private Long    productId;
    private String  imageUrl;
    private String  thumbnailUrl;
    private String  altText;
    private Integer displayOrder;
    private Boolean isPrimary;
    private Boolean active;
    private String  createdAt;
    private String  createdBy;
}
