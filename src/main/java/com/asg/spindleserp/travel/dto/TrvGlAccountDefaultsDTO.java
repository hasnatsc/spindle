package com.asg.spindleserp.travel.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvGlAccountDefaultsDTO {

    private Long id;
    private Long organizationId;

    private Long accountsReceivableId;     private String accountsReceivableDisplay;
    private Long travelRevenueAccountId;   private String travelRevenueAccountDisplay;
    private Long costOfServiceAccountId;   private String costOfServiceAccountDisplay;
    private Long supplierPayableDefaultId; private String supplierPayableDefaultDisplay;
}
