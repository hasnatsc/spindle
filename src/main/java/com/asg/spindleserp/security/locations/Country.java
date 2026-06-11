package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country extends BaseReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 2, nullable = false, unique = true)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
