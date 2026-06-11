package com.asg.spindleserp.production.order;

import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prd_recipes",
        uniqueConstraints = @UniqueConstraint(name = "uk_recipe_org_code", columnNames = {"organization_id", "recipe_code"}),
        indexes = @Index(name = "idx_recipe_prd", columnList = "production_order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionRecipe extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(name = "recipe_code", nullable = false, length = 50)
    private String recipeCode;
    @Column(name = "recipe_name", nullable = false, length = 200)
    private String recipeName;
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;
    @Column(name = "approval_status", length = 30)
    @Builder.Default
    private String approvalStatus = "DRAFT";
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<ProductionRecipeItem> items = new ArrayList<>();

    /**
     * Sum of all item percentages must equal 100
     */
    public void validateBlend() {
        double total = items.stream()
                .mapToDouble(i -> i.getPercentage() != null ? i.getPercentage().doubleValue() : 0)
                .sum();
        if (Math.abs(total - 100.0) > 0.001) {
            throw new RuntimeException("Recipe blend percentages must sum to 100%");
        }
    }
}
