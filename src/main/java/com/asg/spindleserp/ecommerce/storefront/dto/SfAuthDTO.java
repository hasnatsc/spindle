// Path: com/asg/spindleserp/storefront/dto/SfAuthDTO.java
package com.asg.spindleserp.ecommerce.storefront.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SfAuthDTO {
    private Long   id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String password;   // write-only, never returned on read paths
}
