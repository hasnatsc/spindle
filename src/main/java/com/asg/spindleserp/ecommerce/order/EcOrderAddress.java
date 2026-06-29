package com.asg.spindleserp.ecommerce.order;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_order_addresses",
        indexes = @Index(name = "idx_ec_orderaddr_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrderAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AddressType addressType;

    @Column(length = 200)
    private String fullName;
    @Column(length = 30)
    private String phone;
    @Column(length = 200)
    private String email;
    @Column(length = 100)
    private String country;
    @Column(length = 100)
    private String division;
    @Column(length = 100)
    private String district;
    @Column(length = 100)
    private String upazila;
    @Column(length = 20)
    private String postcode;
    @Column(length = 300)
    private String addressLine1;
    @Column(length = 300)
    private String addressLine2;
    @Column(length = 200)
    private String landmark;

    public enum AddressType {BILLING, SHIPPING}
}
