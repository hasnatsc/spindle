package com.asg.spindleserp.travel.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvSupplierCostDTO {

    private Long id;

    @Builder.Default private BigDecimal costAmount = BigDecimal.ZERO;
    @Builder.Default private String currency = "BDT";
    @Builder.Default private String paymentStatus = "UNPAID";
    private String invoiceReference;

    private Long bookingServiceId;
    private Long supplierId; private String supplierDisplay;
}
