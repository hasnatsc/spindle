package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// =============================================================================
// EC SEO METADATA
// =============================================================================

@Entity
@Table(name = "ec_seo_metadata",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_seo",
                columnNames = {"organization_id", "entity_type", "entity_id"}),
        indexes = {
                @Index(name = "idx_ec_seo_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_ec_seo_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcSeoMetadata extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntityType entityType;

    @Column(nullable = false) private Long entityId;

    @Column(length = 255)  private String metaTitle;
    @Column(length = 1000) private String metaKeywords;
    @Column(length = 2000) private String metaDescription;
    @Column(length = 500)  private String canonicalUrl;
    @Column(length = 100)  private String robots;

    // JSON-LD structured data
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String schemaJson;

    // Open Graph
    @Column(length = 255)  private String ogTitle;
    @Column(length = 1000) private String ogDescription;
    @Column(length = 700)  private String ogImage;

    // Twitter Card
    @Column(length = 255)  private String twitterTitle;
    @Column(length = 1000) private String twitterDescription;
    @Column(length = 700)  private String twitterImage;

    public enum EntityType { PRODUCT, CATEGORY, BLOG, PAGE, BRAND }
}

// =============================================================================
// EC URL REDIRECT
// =============================================================================

@Entity
@Table(name = "ec_url_redirects",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_redirect",
                columnNames = {"organization_id", "source_url"}),
        indexes = @Index(name = "idx_ec_redirect_src", columnList = "source_url"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcUrlRedirect extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 700) private String sourceUrl;
    @Column(nullable = false, length = 700) private String destinationUrl;
    @Column(nullable = false) private Integer redirectType;   // 301, 302, 307, 308
    @Builder.Default @Column(nullable = false) private boolean active = true;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}

// =============================================================================
// EC PRODUCT ANALYTICS  (daily conversion funnel — computed by background job)
// DOES NOT duplicate total_sales/total_views from EcProductCatalog (removed)
// These ARE the authoritative counters — EcProductCatalog reads from here.
// =============================================================================

@Entity
@Table(name = "ec_product_analytics",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_prodana",
                columnNames = {"product_id", "analytics_date"}),
        indexes = {
                @Index(name = "idx_ec_prodana_prod", columnList = "product_id"),
                @Index(name = "idx_ec_prodana_date", columnList = "analytics_date")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcProductAnalytics {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Column(nullable = false)
    private LocalDate analyticsDate;

    @Builder.Default @Column(nullable = false) private Integer productViews  = 0;
    @Builder.Default @Column(nullable = false) private Integer wishlistCount = 0;
    @Builder.Default @Column(nullable = false) private Integer cartAdditions = 0;
    @Builder.Default @Column(nullable = false) private Integer purchases      = 0;
    @Builder.Default @Column(nullable = false) private Integer returns        = 0;
    @Builder.Default @Column(precision = 8, scale = 2) private BigDecimal conversionRate = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal revenue       = BigDecimal.ZERO;
}

// =============================================================================
// EC DASHBOARD KPI  (daily snapshot for fast dashboard loading)
// =============================================================================

@Entity
@Table(name = "ec_dashboard_kpis",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_kpi",
                columnNames = {"organization_id", "kpi_date"}),
        indexes = {
                @Index(name = "idx_ec_kpi_org",  columnList = "organization_id"),
                @Index(name = "idx_ec_kpi_date", columnList = "kpi_date")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcDashboardKpi extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private LocalDate kpiDate;

    @Column(precision = 18, scale = 2) private BigDecimal totalSales;
    private Integer totalOrders;
    private Integer totalCustomers;
    private Integer newCustomers;
    @Column(precision = 18, scale = 2) private BigDecimal averageOrderValue;
    @Column(precision = 8, scale = 2)  private BigDecimal conversionRate;
    @Column(precision = 8, scale = 2)  private BigDecimal abandonedCartRate;
    @Column(precision = 8, scale = 2)  private BigDecimal returnRate;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}

// =============================================================================
// EC AUDIT LOG  (ecommerce admin action audit — separate from sys_audit_log)
// =============================================================================

@Entity
@Table(name = "ec_audit_logs",
        indexes = {
                @Index(name = "idx_ec_audit_entity", columnList = "entity_name,entity_id"),
                @Index(name = "idx_ec_audit_user",   columnList = "user_id"),
                @Index(name = "idx_ec_audit_org",    columnList = "organization_id"),
                @Index(name = "idx_ec_audit_time",   columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcAuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id") private Long organizationId;

    // ERP staff user performing the action (null for automated system actions)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100) private String entityName;
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AuditAction action;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb") private String oldData;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb") private String newData;

    @Column(length = 50)  private String ipAddress;
    @Column(columnDefinition = "text") private String userAgent;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    public enum AuditAction { INSERT, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, IMPORT }
}
