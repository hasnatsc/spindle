package com.asg.spindleserp.organization.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "org_organizations",
        indexes = {
                @Index(name = "idx_org_code", columnList = "code"),
                @Index(name = "idx_org_active", columnList = "is_active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String nameBn;
    @Column(columnDefinition = "text")
    private String about;
    @Column(columnDefinition = "text")
    private String address;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String state;
    @Column(length = 100)
    private String country;
    @Column(length = 20)
    private String postalCode;
    @Column(length = 20)
    private String phone;
    @Column(length = 100)
    private String email;
    @Column(length = 255)
    private String website;
    @Column(length = 500)
    private String logoUrl;
    private LocalDate establishedDate;
    @Column(length = 50)
    private String taxId;
    @Column(length = 50)
    private String vatNo;
    @Column(length = 50)
    private String binNo;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
