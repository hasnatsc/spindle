package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trv_booking_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvBookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @PrePersist
    void onCreate() { changedAt = LocalDateTime.now(); }
}
