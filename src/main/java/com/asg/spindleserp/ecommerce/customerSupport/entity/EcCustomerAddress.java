package com.asg.spindleserp.ecommerce.customerSupport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_customer_addresses",
        indexes = @Index(name = "idx_ec_custaddr_cust", columnList = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AddressType addressType;

    @Column(length = 200)
    private String contactPerson;
    @Column(length = 30)
    private String contactPhone;

    @Builder.Default
    @Column(length = 100)
    private String country = "Bangladesh";

    @Column(length = 100)
    private String division;
    @Column(length = 100)
    private String district;
    @Column(length = 100)
    private String upazila;
    @Column(length = 20)
    private String postCode;
    @Column(length = 150)
    private String area;
    @Column(length = 300)
    private String addressLine1;
    @Column(length = 300)
    private String addressLine2;
    @Column(length = 200)
    private String landmark;

    @Column(precision = 12, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 12, scale = 8)
    private BigDecimal longitude;

    @Builder.Default
    @Column(nullable = false)
    private boolean defaultShipping = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean defaultBilling = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AddressType {HOME, OFFICE, BILLING, SHIPPING, OTHER}
}
