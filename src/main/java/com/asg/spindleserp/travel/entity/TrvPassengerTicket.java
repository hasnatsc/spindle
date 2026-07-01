package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_passenger_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPassengerTicket {

    public enum Status { ISSUED, CANCELLED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", length = 50)
    private String ticketNumber;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    @Column(name = "baggage_allowance", length = 100)
    private String baggageAllowance;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ISSUED;

    @Column(name = "air_ticket_id", nullable = false)
    private Long airTicketId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;
}
