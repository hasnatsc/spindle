// Path: com/asg/spindleserp/ecommerce/storefront/dto/SfBannerDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfBannerDTO {
    private Long id;
    private String title;
    private String subTitle;
    private String description;
    private String imageUrl;
    private String mobileImageUrl;
    private String buttonText;
    private String buttonUrl;
    @Builder.Default private Boolean openInNewTab = Boolean.FALSE;
}
