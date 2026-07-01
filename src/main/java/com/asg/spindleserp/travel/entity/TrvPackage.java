package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trv_packages", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_pkg_org_code", columnNames = {"organization_id", "package_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPackage extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_code", nullable = false, length = 30)
    private String packageCode;

    @Column(name = "package_name", nullable = false, length = 200)
    private String packageName;

    @Column(name = "destination", length = 150)
    private String destination;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "duration_nights")
    private Integer durationNights;

    @Builder.Default
    @Column(name = "base_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Column(name = "description", length = 2000)
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(mappedBy = "packageEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TrvPackageItineraryDay> itineraryDays = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "packageEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TrvPackageInclusion> inclusions = new ArrayList<>();
}
