package com.asg.spindleserp.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FIX: implements Serializable
 *
 * User has @ManyToOne(EAGER) Organization.
 * When Spring Session JDBC serializes User (inside CustomUserDetails),
 * the entire object graph must be Serializable — including Organization.
 *
 * Add @Serial serialVersionUID to every entity in the security graph.
 */
@Entity
@Table(name = "org_organizations",
    indexes = {
        @Index(name = "idx_org_code",   columnList = "code"),
        @Index(name = "idx_org_active", columnList = "is_active")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization implements Serializable {   // ✅ FIX

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String nameBn;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(length = 100) private String country;
    @Column(length = 20)  private String postalCode;
    @Column(length = 20)  private String phone;
    @Column(length = 100) private String email;
    @Column(length = 255) private String website;
    @Column(length = 500) private String logoUrl;

    private LocalDate establishedDate;

    @Column(length = 50) private String taxId;
    @Column(length = 50) private String vatNo;
    @Column(length = 50) private String binNo;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(length = 100) private String createdBy;
    @Column(length = 100) private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
