package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "trv_tours", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_tour_org_code", columnNames = {"organization_id", "tour_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvTour extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tour_code", nullable = false, length = 30)
    private String tourCode;

    @Column(name = "tour_name", nullable = false, length = 200)
    private String tourName;

    @Column(name = "destination", length = 150)
    private String destination;

    @Column(name = "duration_hours", precision = 6, scale = 2)
    private BigDecimal durationHours;

    @Builder.Default
    @Column(name = "base_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
