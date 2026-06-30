// ============================================================
// Path: com/asg/spindleserp/ecommerce/dto/  — remaining DTOs
// ============================================================

// ── EcCouponDTO ──────────────────────────────────────────────
// Path: com/asg/spindleserp/ecommerce/dto/EcCouponDTO.java
package com.asg.spindleserp.ecommerce.campaign.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCouponDTO {
    private Long   id;
    @NotBlank @Size(max = 50)
    private String couponCode;
    @Size(max = 200)
    private String couponName;
    private String description;
    private String discountType;   // PERCENTAGE / FIXED
    private BigDecimal discountValue;
    private BigDecimal minimumOrder;
    private BigDecimal maximumDiscount;
    private Integer    usageLimit;
    private Integer    usagePerCustomer;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean    active;
    private String     createdAt;
    private String     updatedAt;
    private String     createdBy;
}
