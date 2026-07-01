package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_visa_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvVisaDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_name", nullable = false, length = 150)
    private String documentName;

    @Builder.Default
    @Column(name = "is_received", nullable = false)
    private Boolean isReceived = false;

    @Column(name = "remarks", length = 300)
    private String remarks;

    @Column(name = "visa_application_id", nullable = false)
    private Long visaApplicationId;
}
