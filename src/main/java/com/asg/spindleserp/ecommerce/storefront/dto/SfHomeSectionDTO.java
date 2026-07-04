// Path: com/asg/spindleserp/ecommerce/storefront/dto/SfHomeSectionDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfHomeSectionDTO {
    private Long id;
    private String sectionCode;
    private String sectionName;
    private String sectionTitle;
    private String sectionSubtitle;
    private List<SfProductCardDTO> products;
}
