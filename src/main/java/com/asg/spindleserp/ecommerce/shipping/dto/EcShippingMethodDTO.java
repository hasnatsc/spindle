// Path: com/asg/spindleserp/ecommerce/dto/EcShippingMethodDTO.java
package com.asg.spindleserp.ecommerce.shipping.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShippingMethodDTO {
    private Long   id;
    @NotBlank @Size(max = 30)
    private String methodCode;
    @NotBlank @Size(max = 100)
    private String methodName;
    @Size(max = 100)
    private String courierName;
    private Integer    estimatedDays;
    private BigDecimal baseCharge;
    private BigDecimal chargePerKg;
    private Boolean    cashOnDelivery;
    private Boolean    apiEnabled;
    private Boolean    active;
    private String     createdAt;
    private String     updatedAt;
    private String     createdBy;
}
