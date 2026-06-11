package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * SubAccount — JOINED TABLE INHERITANCE base.
 * sub_account_type discriminates: BANK | CASH | CUSTOMER | SUPPLIER | GENERAL
 * Child tables: BankAccount, CashAccount, Customer (acc_customers), Supplier (acc_suppliers)
 */
@Entity
@Table(name = "acc_chart_of_accounts_sub",
        uniqueConstraints = @UniqueConstraint(name = "uk_sub_org_code", columnNames = {"organization_id", "sub_account_code"}),
        indexes = {
                @Index(name = "idx_sub_org", columnList = "organization_id"),
                @Index(name = "idx_sub_type", columnList = "sub_account_type")
        })
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "sub_account_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubAccount extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_account_id", nullable = false)
    private Account mainAccount;

    @Column(name = "sub_account_code", nullable = false, length = 50)
    private String subAccountCode;
    @Column(name = "sub_account_name", nullable = false, length = 200)
    private String subAccountName;

    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(length = 20)
    @Builder.Default
    private String currency = "BDT";
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "contact_person", length = 200)
    private String contactPerson;
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    @Column(name = "contact_email", length = 100)
    private String contactEmail;
    @Column(length = 500)
    private String address;
    @Column(length = 50)
    private String city;
    @Column(length = 50)
    private String country;
    @Column(name = "tax_id", length = 50)
    private String taxId;
    @Column(name = "vat_registration_no", length = 50)
    private String vatRegistrationNo;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
