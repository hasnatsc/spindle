// Path: com/asg/spindleserp/ecommerce/dto/EcHomeSectionDTO.java
package com.asg.spindleserp.ecommerce;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcHomeSectionDTO {
    private Long    id;
    @NotBlank @Size(max = 50)
    private String  sectionCode;
    @NotBlank @Size(max = 200)
    private String  sectionName;
    @Size(max = 300)
    private String  sectionTitle;
    @Size(max = 500)
    private String  sectionSubtitle;
    private String  sectionType;     // FEATURED / NEW_ARRIVAL / BEST_SELLER … CUSTOM
    private Integer displayOrder;
    private Integer maxProducts;
    private Boolean active;

    @Builder.Default
    private List<SectionProductDTO> sectionProducts = new ArrayList<>();

    private String createdAt;
    private String updatedAt;
    private String createdBy;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SectionProductDTO {
        private Long    id;
        private Long    productId;
        private String  productTitle;   // denorm
        private String  productSlug;    // denorm
        private Integer displayOrder;
    }
}
