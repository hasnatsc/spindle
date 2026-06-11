package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "eco_tax_classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_tax_cls", columnNames = {"store_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoTaxClass implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 100)
    private String name;
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
