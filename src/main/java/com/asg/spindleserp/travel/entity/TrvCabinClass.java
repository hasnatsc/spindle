package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_cabin_classes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvCabinClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", nullable = false, length = 10, unique = true)
    private String classCode;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;
}
