package com.asg.spindleserp.ecommerce.settings;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ec_tax_rules",
        indexes = {
                @Index(name = "idx_ec_taxrule_class", columnList = "tax_class_id"),
                @Index(name = "idx_ec_taxrule_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcTaxRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id")
    private EcTaxClass taxClass;

    @Builder.Default
    @Column(length = 100)
    private String country = "Bangladesh";
    @Column(length = 100)
    private String division;
    @Column(length = 100)
    private String district;
    @Column(length = 100)
    private String taxName;
    @Column(precision = 8, scale = 2)
    private BigDecimal taxPercent;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
