package com.asg.spindleserp.ecommerce.customerSupport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_customer_otp",
        indexes = {
                @Index(name = "idx_ec_otp_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_otp_phone", columnList = "phone"),
                @Index(name = "idx_ec_otp_email", columnList = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCustomerOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(length = 30)
    private String phone;
    @Column(length = 200)
    private String email;
    @Column(length = 10)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OtpType otpType;

    private LocalDateTime expiryTime;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum OtpType {LOGIN, REGISTER, FORGOT_PASSWORD, VERIFY_PHONE, VERIFY_EMAIL}
}
