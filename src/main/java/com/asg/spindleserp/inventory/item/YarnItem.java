package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yarn_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YarnItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_type_id")
    private YarnType yarnType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_count_id")
    private YarnCount yarnCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_ply_id")
    private YarnPly yarnPly;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_blend_id")
    private YarnBlend yarnBlend;

    @Column(name = "quality_grade", length = 50)
    private String qualityGrade;
    @Column(name = "display_name", length = 500)
    private String displayName;

    /**
     * Builds display name: "30/2 Combed 100% Cotton"
     */
    @PostLoad
    @PostPersist
    @PostUpdate
    public void buildDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (yarnCount != null && yarnPly != null)
            sb.append(yarnCount.getCountName()).append("/").append(yarnPly.getPlyNumber());
        else if (yarnCount != null) sb.append(yarnCount.getCountName());
        if (yarnType != null && !yarnType.getTypeName().isBlank()) sb.append(" ").append(yarnType.getTypeName());
        if (yarnBlend != null && !yarnBlend.getBlendName().isBlank()) sb.append(" ").append(yarnBlend.getBlendName());
        this.displayName = sb.toString().trim();
    }
}
