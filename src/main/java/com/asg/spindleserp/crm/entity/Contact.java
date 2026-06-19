package com.asg.spindleserp.crm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_contacts",
        indexes = {
                @Index(name = "idx_crc_customer", columnList = "customer_id"),
                @Index(name = "idx_crc_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private ChartOfAccountSub customer;

    @Column(nullable = false, length = 100)
    private String firstName;
    @Column(length = 100)
    private String lastName;
    @Column(length = 100)
    private String designation;
    @Column(length = 100)
    private String department;
    @Column(length = 100)
    private String email;
    @Column(length = 20)
    private String phone;
    @Column(length = 20)
    private String mobile;
    @Column(length = 20)
    private String whatsapp;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPrimary = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(columnDefinition = "text")
    private String notes;
}
