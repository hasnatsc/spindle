package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_hotels", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_hotel_org_code", columnNames = {"organization_id", "hotel_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_code", nullable = false, length = 30)
    private String hotelCode;

    @Column(name = "hotel_name", nullable = false, length = 200)
    private String hotelName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "star_rating")
    private Integer starRating;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Soft FK to trv_hotel_categories — same-module reference kept plain to avoid lazy-init issues. */
    @Column(name = "category_id")
    private Long categoryId;
}
