package com.asg.spindleserp.hrm.pims;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hrm_employee_addresses",
        indexes = @Index(name = "idx_eaddr_emp", columnList = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAddress implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default
    private AddressType addressType = AddressType.PRESENT; // PRESENT|PERMANENT

    @Column(name = "address_line1", length = 200)
    private String addressLine1;
    @Column(name = "address_line2", length = 200)
    private String addressLine2;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String district;
    @Column(length = 100)
    private String state;
    @Column(length = 100)
    private String country;
    @Column(name = "postal_code", length = 20)
    private String postalCode;
}
