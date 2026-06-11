package com.asg.spindleserp.production.order;

import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prd_recipe_items",
        indexes = {
                @Index(name = "idx_ritem_recipe", columnList = "yarn_recipe_id"),
                @Index(name = "idx_ritem_raw", columnList = "raw_material_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionRecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_recipe_id", nullable = false)
    private ProductionRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private InventoryItem rawMaterial;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "recipeItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductionRecipeItemLot> lots = new ArrayList<>();
}
