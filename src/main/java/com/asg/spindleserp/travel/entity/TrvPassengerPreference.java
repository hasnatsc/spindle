package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_passenger_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPassengerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_preference", length = 100)
    private String mealPreference;

    @Column(name = "seat_preference", length = 100)
    private String seatPreference;

    @Column(name = "special_assistance", length = 300)
    private String specialAssistance;

    @Column(name = "dietary_restriction", length = 300)
    private String dietaryRestriction;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "passenger_id", nullable = false, unique = true)
    private Long passengerId;
}
