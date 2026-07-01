package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trv_booking_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvBookingNote {

    public enum NoteType { INTERNAL, CUSTOMER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Lob
    @Column(name = "note_text", nullable = false)
    private String noteText;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 20)
    private NoteType noteType = NoteType.INTERNAL;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
