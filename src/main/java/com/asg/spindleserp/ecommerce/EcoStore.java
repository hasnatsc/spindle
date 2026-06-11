package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eco_stores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_org_code", columnNames = {"organization_id", "store_code"}),
                @UniqueConstraint(name = "uk_store_slug", columnNames = {"store_slug"})
        },
        indexes = {
                @Index(name = "idx_store_org", columnList = "organization_id"),
                @Index(name = "idx_store_slug", columnList = "store_slug")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoStore extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private BusinessUnit businessUnit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_account_id")
    private Account arAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_account_id")
    private Account revenueAccount;

    @Column(name = "store_code", nullable = false, length = 50)
    private String storeCode;
    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;
    @Column(name = "store_name_bn", length = 200)
    private String storeNameBn;
    @Column(name = "store_slug", nullable = false, length = 100)
    private String storeSlug;
    @Column(name = "store_type", nullable = false, length = 30)
    @Builder.Default
    private String storeType = "B2C"; // B2C|B2B|WHOLESALE|MARKETPLACE
    @Column(length = 300)
    private String domain;
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "contact_email", length = 100)
    private String contactEmail;
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    @Column(name = "default_currency", nullable = false, length = 3)
    @Builder.Default
    private String defaultCurrency = "BDT";
    @Column(name = "default_language", nullable = false, length = 10)
    @Builder.Default
    private String defaultLanguage = "en";
    @Builder.Default
    @Column(name = "tax_inclusive", nullable = false)
    private Boolean taxInclusive = false;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_maintenance", nullable = false)
    private Boolean isMaintenance = false;
    @Column(name = "maintenance_message", columnDefinition = "TEXT")
    private String maintenanceMessage;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
