package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trv_meal_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvMealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "plan_code", nullable = false, length = 10, unique = true)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "description", length = 300)
    private String description;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
