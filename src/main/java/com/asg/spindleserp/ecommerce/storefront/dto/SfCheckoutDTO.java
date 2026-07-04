// Path: com/asg/spindleserp/ecommerce/storefront/dto/SfCheckoutDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;

/** Request body of POST /checkout/place-order. Either addressId OR the inline address fields. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfCheckoutDTO {
    private Long addressId;          // saved ec_customer_addresses.id (must belong to the customer)

    // inline "new address" fields
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String area;
    private String landmark;
    private String upazila;
    private String district;
    private String division;
    private String postCode;
    @Builder.Default private Boolean saveAddress = Boolean.FALSE;

    private String paymentMethod;    // COD (online gateways = ★ future seam)
    private String customerNote;
}
