package com.asg.spindleserp.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_addresses",
        indexes = @Index(name = "idx_hea_emp", columnList = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeAddress.AddressType addressType;

    @Column(length = 200)
    private String addressLine1;
    @Column(length = 200)
    private String addressLine2;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String district;
    @Builder.Default
    @Column(length = 100)
    private String country = "Bangladesh";
    @Column(length = 20)
    private String postalCode;
    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AddressType {PRESENT, PERMANENT, OFFICE}
}
