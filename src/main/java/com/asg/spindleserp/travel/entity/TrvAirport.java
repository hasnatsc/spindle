package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trv_airports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvAirport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "airport_code", nullable = false, length = 3, unique = true)
    private String airportCode;

    @Column(name = "airport_name", nullable = false, length = 200)
    private String airportName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
