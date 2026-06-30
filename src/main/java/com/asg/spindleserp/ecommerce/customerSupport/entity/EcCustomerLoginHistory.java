package com.asg.spindleserp.ecommerce.customerSupport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_customer_login_history",
        indexes = {
                @Index(name = "idx_ec_login_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_login_time", columnList = "login_time")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCustomerLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    @Column(length = 50)
    private String ipAddress;
    @Column(length = 150)
    private String browser;
    @Column(length = 150)
    private String operatingSystem;
    @Column(length = 150)
    private String device;
    @Column(length = 20)
    private String loginStatus;   // SUCCESS | FAILED
    @Column(length = 50)
    private String loginSource;   // WEB | ANDROID | IOS
}
