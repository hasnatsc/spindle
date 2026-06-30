// Path: com/asg/spindleserp/ecommerce/dto/EcGlAccountDefaultsDTO.java
package com.asg.spindleserp.ecommerce;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcGlAccountDefaultsDTO {
    private Long   id;
    private Long   salesRevenueAccountId;
    private String salesRevenueAccountName;
    private Long   salesReturnsAccountId;
    private String salesReturnsAccountName;
    private Long   cogsAccountId;
    private String cogsAccountName;
    private Long   accountsReceivableId;
    private String accountsReceivableName;
    private Long   vatPayableAccountId;
    private String vatPayableAccountName;
    private Long   discountExpenseAccountId;
    private String discountExpenseAccountName;
    private Long   shippingIncomeAccountId;
    private String shippingIncomeAccountName;
    private Long   defaultBankSubAccountId;
    private String defaultBankSubAccountName;
    private String updatedAt;
    private String updatedBy;
}
