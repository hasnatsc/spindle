package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fa_asset_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_fa_cat_org_code", columnNames = {"organization_id", "code"}),
        indexes = @Index(name = "idx_facat_parent", columnList = "parent_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCategory extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AssetCategory parent;

    // GL account links
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_asset_account_id")
    private Account glAssetAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_dep_exp_account_id")
    private Account glDepExpAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_accum_dep_account_id")
    private Account glAccumDepAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_disposal_account_id")
    private Account glDisposalAccount;

    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "default_dep_method", length = 30)
    @Builder.Default
    private String defaultDepMethod = "STRAIGHT_LINE";
    @Column(name = "default_useful_life")
    private Integer defaultUsefulLife;
    @Column(name = "default_dep_rate", precision = 5, scale = 2)
    private BigDecimal defaultDepRate;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
