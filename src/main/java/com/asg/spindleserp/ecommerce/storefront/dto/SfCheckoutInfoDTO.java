// Path: com/asg/spindleserp/ecommerce/storefront/dto/SfCheckoutInfoDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;

import java.io.Serializable;

/**
 * SfCheckoutInfoDTO — the shipping info captured at checkout step 2
 * (/checkout/info) and held in the HTTP session (key "SF_CHECKOUT_INFO")
 * until the order is placed at step 3. Serializable because Spring Session
 * JDBC persists session attributes.
 *
 * Either addressId (a saved ec_customer_addresses row belonging to the
 * customer) OR the inline fields are populated — mirroring SfCheckoutDTO,
 * into which this is merged at place-order time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SfCheckoutInfoDTO implements Serializable {

    private Long addressId;

    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String area;
    private String upazila;
    private String district;
    private String division;
    private String postCode;
    @Builder.Default
    private Boolean saveAddress = Boolean.FALSE;

    /** One-line display string for the payment page's "Deliver to" block. */
    public String toAddressText() {
        StringBuilder sb = new StringBuilder();
        append(sb, addressLine1);
        append(sb, addressLine2);
        append(sb, area);
        append(sb, upazila);
        append(sb, district);
        append(sb, division);
        if (postCode != null && !postCode.isBlank()) sb.append(" — ").append(postCode.trim());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(part.trim());
    }
}
