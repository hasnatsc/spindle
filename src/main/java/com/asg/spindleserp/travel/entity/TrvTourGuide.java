package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_tour_guides")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvTourGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_name", nullable = false, length = 150)
    private String guideName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "languages", length = 200)
    private String languages;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
