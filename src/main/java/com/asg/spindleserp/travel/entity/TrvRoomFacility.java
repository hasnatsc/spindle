package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trv_room_facilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvRoomFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "facility_name", nullable = false, length = 150)
    private String facilityName;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
