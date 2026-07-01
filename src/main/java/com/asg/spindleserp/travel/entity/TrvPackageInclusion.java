package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_package_inclusions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPackageInclusion {

    public enum InclusionType { INCLUDED, EXCLUDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "inclusion_type", nullable = false, length = 10)
    private InclusionType inclusionType = InclusionType.INCLUDED;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private TrvPackage packageEntity;
}
