package com.asg.spindleserp.ecommerce.settings;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_tax_classes",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_taxclass",
                columnNames = {"organization_id", "class_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcTaxClass extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String classCode;
    @Column(length = 150)
    private String className;
    @Column(columnDefinition = "text")
    private String description;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
