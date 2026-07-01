package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "trv_passengers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPassenger extends BaseEntity implements Serializable {

    public enum Gender { MALE, FEMALE, OTHER }

    public enum PassengerType { ADULT, CHILD, INFANT }

    @Column(name = "title", length = 10)
    private String title;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type", nullable = false, length = 10)
    private PassengerType passengerType = PassengerType.ADULT;

    @Builder.Default
    @Column(name = "is_lead_passenger", nullable = false)
    private Boolean isLeadPassenger = false;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private TrvBooking booking;
}
