package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_customer_addresses",
        indexes = @Index(name = "idx_eaddr_cust", columnList = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoCustomerAddress implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default
    private String addressType = "SHIPPING"; // SHIPPING|BILLING|BOTH
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    @Column(name = "first_name", length = 100)
    private String firstName;
    @Column(name = "last_name", length = 100)
    private String lastName;
    @Column(length = 200)
    private String company;
    @Column(name = "address_line1", nullable = false, length = 300)
    private String addressLine1;
    @Column(name = "address_line2", length = 300)
    private String addressLine2;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(length = 100)
    private String district;
    @Column(length = 100)
    private String state;
    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "BD";
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(length = 20)
    private String phone;
    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
