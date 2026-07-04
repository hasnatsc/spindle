// Path: com/asg/spindleserp/ecommerce/storefront/dto/SfAddressDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfAddressDTO {
    private Long id;
    private String addressType;      // HOME / OFFICE / SHIPPING / BILLING / OTHER
    private String contactPerson;
    private String contactPhone;
    private String addressLine1;
    private String addressLine2;
    private String area;
    private String landmark;
    private String upazila;
    private String district;
    private String division;
    private String postCode;
    private String country;
    @Builder.Default private Boolean defaultShipping = Boolean.FALSE;
    @Builder.Default private Boolean defaultBilling  = Boolean.FALSE;
}
