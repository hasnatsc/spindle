package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trv_airlines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvAirline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "airline_code", nullable = false, length = 3, unique = true)
    private String airlineCode;

    @Column(name = "airline_name", nullable = false, length = 150)
    private String airlineName;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
