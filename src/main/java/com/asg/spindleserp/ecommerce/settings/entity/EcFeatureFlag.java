package com.asg.spindleserp.ecommerce.settings.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_feature_flags",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_feature",
                columnNames = {"organization_id", "feature_name"}),
        indexes = @Index(name = "idx_ec_feature_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcFeatureFlag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String featureName;
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = false;
    @Column(length = 500)
    private String notes;
}
