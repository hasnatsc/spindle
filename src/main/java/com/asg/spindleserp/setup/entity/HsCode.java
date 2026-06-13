package com.asg.spindleserp.setup.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "com_hs_codes",
        uniqueConstraints = @UniqueConstraint(name = "uq_hs_org_code",
                columnNames = {"organization_id", "hs_code"}),
        indexes = @Index(name = "idx_hscode_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)
    private String hsCode;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 200)
    private String shortDescription;

    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private HsCode.HsType hsType = HsCode.HsType.BOTH;

    @Column(precision = 6, scale = 2)
    private BigDecimal vatPercent;
    @Column(precision = 6, scale = 2)
    private BigDecimal customsDutyPercent;
    @Column(precision = 6, scale = 2)
    private BigDecimal supplementaryDutyPercent;
    @Column(precision = 6, scale = 2)
    private BigDecimal aitPercent;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isBondedAllowed = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean requiresImportPermit = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean requiresExportPermit = false;

    public enum HsType {EXPORT, IMPORT, BOTH}
}
