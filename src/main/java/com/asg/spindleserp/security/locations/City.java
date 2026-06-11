package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City extends BaseReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "name_bn", length = 150)
    private String nameBn;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
