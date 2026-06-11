package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.dummy.EcoAttributeValue;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;

@Entity
@Table(name = "eco_attribute_groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_atg_store_slug", columnNames = {"store_id", "slug"}),
        indexes = @Index(name = "idx_atg_store", columnList = "store_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoAttributeGroup implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 100)
    private String name;   // Colour | Count | Ply | Size
    @Column(name = "name_bn", length = 100)
    private String nameBn;
    @Column(nullable = false, length = 100)
    private String slug;
    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;
    @Builder.Default
    @Column(name = "is_variation", nullable = false)
    private Boolean isVariation = true;
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    @OneToMany(mappedBy = "attributeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EcoAttributeValue> values = new ArrayList<>();
}
