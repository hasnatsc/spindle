package com.asg.spindleserp.fixedassets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fa_asset_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_fac_org_code",
                columnNames = {"organization_id", "code"}),
        indexes = {
                @Index(name = "idx_fac_org", columnList = "organization_id"),
                @Index(name = "idx_fac_parent", columnList = "parent_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AssetCategory parent;

    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "text")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetCategory.DepreciationMethod defaultDepMethod = AssetCategory.DepreciationMethod.STRAIGHT_LINE;

    private Integer defaultUsefulLifeYears;
    @Column(precision = 5, scale = 2)
    private BigDecimal defaultDepRate;
    @Column(precision = 5, scale = 2)
    private BigDecimal defaultResidualPct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_asset_account_id")
    private ChartOfAccount glAssetAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_dep_exp_account_id")
    private ChartOfAccount glDepExpAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_accum_dep_account_id")
    private ChartOfAccount glAccumDepAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_disposal_account_id")
    private ChartOfAccount glDisposalAccount;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum DepreciationMethod {STRAIGHT_LINE, DECLINING_BALANCE, UNITS_OF_PRODUCTION}
}
