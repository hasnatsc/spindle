package com.asg.spindleserp.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inv_item_uom",
    uniqueConstraints = @UniqueConstraint(name = "uq_uom_org_code", columnNames = {"organization_id", "code"}), indexes = @Index(name = "idx_uom_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemUom {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)  private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 20) private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UomCategory category;

    @Builder.Default @Column(nullable = false) private boolean isBaseUnit = false;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Builder.Default @Column(nullable = false) private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum UomCategory { WEIGHT, COUNT, LENGTH, VOLUME, AREA, PACKING, UNIT }
}

