package com.asg.spindleserp.ecommerce.customerSupport;

import com.asg.spindleserp.ecommerce.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_customer_devices",
        indexes = @Index(name = "idx_ec_device_cust", columnList = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCustomerDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Column(length = 200)
    private String deviceUuid;
    @Column(length = 200)
    private String deviceName;
    @Column(length = 50)
    private String deviceType;
    @Column(length = 100)
    private String osName;
    @Column(length = 50)
    private String appVersion;
    @Column(columnDefinition = "text")
    private String pushToken;
    private LocalDateTime lastActive;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
