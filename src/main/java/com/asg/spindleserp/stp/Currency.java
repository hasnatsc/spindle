package com.asg.spindleserp.stp;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency extends BaseReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 3, nullable = false, unique = true)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 10)
    private String symbol;
    @Builder.Default
    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces = 2;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
