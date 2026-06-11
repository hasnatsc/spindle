package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_business_document_line_lots",
        indexes = {
                @Index(name = "idx_gdll_line", columnList = "document_line_id"),
                @Index(name = "idx_gdll_lot", columnList = "lot_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDocumentLineLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_line_id", nullable = false)
    private BusinessDocumentLine documentLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private GlobalInventoryLot lot;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Column(name = "bale_quantity", precision = 12, scale = 3)
    private BigDecimal baleQuantity;
    @Column
    private Integer bags;
    @Column(name = "bag_capacity", length = 20)
    @Builder.Default
    private String bagCapacity = "CUSTOM"; // CONE_25|CONE_50|CUSTOM
    @Column(name = "bag_weight", precision = 12, scale = 3)
    private BigDecimal bagWeight;
    @Column(name = "bag_quantity")
    private Integer bagQuantity;
    @Column(name = "cones_per_bag")
    private Integer conesPerBag;
    @Column(name = "cone_quantity")
    private Integer coneQuantity;
    @Column(name = "actual_weight", precision = 12, scale = 3)
    private BigDecimal actualWeight;
    @Column(name = "net_weight", precision = 12, scale = 3)
    private BigDecimal netWeight;
    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;
    @Column(name = "total_cost", precision = 18, scale = 2)
    private BigDecimal totalCost;
    @Column(name = "quality_remarks", columnDefinition = "TEXT")
    private String qualityRemarks;
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
