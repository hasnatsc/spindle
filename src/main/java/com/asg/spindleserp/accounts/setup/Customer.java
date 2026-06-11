package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_org_code", columnNames = {"organization_id", "customer_code"}))
@DiscriminatorValue("CUSTOMER")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Customer extends SubAccount {

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;
    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit;
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;
    @Column(name = "credit_days")
    private Integer creditDays;
    @Column(name = "sales_representative", length = 100)
    private String salesRepresentative;
    @Column(name = "preferred_contact", length = 50)
    private String preferredContact;
    @Column(name = "customer_group", length = 50)
    private String customerGroup;
    @Builder.Default
    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;
    @Builder.Default
    @Column(name = "is_export_customer")
    private Boolean isExportCustomer = false;
}
