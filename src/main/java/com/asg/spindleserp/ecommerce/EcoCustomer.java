package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eco_customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_ecust_store_email", columnNames = {"store_id", "email"}),
        indexes = {
                @Index(name = "idx_ecust_store", columnList = "store_id"),
                @Index(name = "idx_ecust_email", columnList = "store_id,email"),
                @Index(name = "idx_ecust_sub", columnList = "sub_account_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoCustomer extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_account_id", unique = true)
    private SubAccount subAccount;         // AR ledger link (created on first confirmed order)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(length = 20)
    private String phone;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(length = 10)
    private String gender;
    @Column(name = "customer_type", nullable = false, length = 20)
    @Builder.Default
    private String customerType = "REGISTERED"; // GUEST|REGISTERED|B2B|WHOLESALE
    @Column(name = "preferred_currency", length = 3)
    @Builder.Default
    private String preferredCurrency = "BDT";
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en";
    @Builder.Default
    @Column(name = "email_marketing", nullable = false)
    private Boolean emailMarketing = true;
    @Builder.Default
    @Column(name = "sms_marketing", nullable = false)
    private Boolean smsMarketing = true;
    @Builder.Default
    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders = 0;
    @Builder.Default
    @Column(name = "total_spent", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;
    @Column(name = "last_order_date")
    private LocalDate lastOrderDate;
    @Column(name = "company_name", length = 200)
    private String companyName;
    @Column(name = "tax_id", length = 50)
    private String taxId;
    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit;
    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;
    @Column(name = "email_verified_at")
    private java.time.LocalDateTime emailVerifiedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
