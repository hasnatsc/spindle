package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trv_visa_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvVisaType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "visa_category", nullable = false, length = 100)
    private String visaCategory;

    @Column(name = "processing_days")
    private Integer processingDays;

    @Builder.Default
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Column(name = "description", length = 500)
    private String description;
}
