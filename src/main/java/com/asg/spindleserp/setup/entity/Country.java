package com.asg.spindleserp.setup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stp_location_countries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Country {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(nullable = false, unique = true, length = 3)
    private String isoCode;

    @Column(nullable = false, length = 2)
    private String isoCode2;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150) private String nameNative;
    @Column(length = 10)  private String phoneCode;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
}