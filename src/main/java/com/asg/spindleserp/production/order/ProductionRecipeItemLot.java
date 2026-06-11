package com.asg.spindleserp.production.order;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prd_recipe_item_lots",
        indexes = @Index(name = "idx_rlot_item", columnList = "production_recipe_item_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionRecipeItemLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_recipe_item_id", nullable = false)
    private ProductionRecipeItem recipeItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_lot_id", nullable = false)
    private GlobalInventoryLot inventoryLot;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;
    @Builder.Default
    @Column(name = "buffer_percent", precision = 5, scale = 2)
    private BigDecimal bufferPercent = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "waste_factor_percent", precision = 5, scale = 2)
    private BigDecimal wasteFactorPercent = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
