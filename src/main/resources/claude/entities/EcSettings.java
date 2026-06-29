package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// =============================================================================
// EC SETTINGS  (key-value store per org)
// =============================================================================

@Entity
@Table(name = "ec_settings",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_setting",
                columnNames = {"organization_id", "setting_group", "setting_key"}),
        indexes = @Index(name = "idx_ec_setting_group", columnList = "organization_id,setting_group"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcSetting extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100) private String settingGroup;
    @Column(length = 150) private String settingKey;
    @Column(columnDefinition = "text") private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DataType dataType;

    @Column(columnDefinition = "text") private String description;
    @Builder.Default @Column(nullable = false) private boolean editable = true;

    public enum DataType { STRING, NUMBER, BOOLEAN, JSON }
}

// =============================================================================
// EC TAX CLASS
// =============================================================================

@Entity
@Table(name = "ec_tax_classes",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_taxclass",
                columnNames = {"organization_id", "class_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcTaxClass extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)  private String classCode;
    @Column(length = 150) private String className;
    @Column(columnDefinition = "text") private String description;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC TAX RULE  (zone + class → rate matrix)
// =============================================================================

@Entity
@Table(name = "ec_tax_rules",
        indexes = {
                @Index(name = "idx_ec_taxrule_class", columnList = "tax_class_id"),
                @Index(name = "idx_ec_taxrule_org",   columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcTaxRule extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id")
    private EcTaxClass taxClass;

    @Builder.Default @Column(length = 100) private String country = "Bangladesh";
    @Column(length = 100) private String division;
    @Column(length = 100) private String district;
    @Column(length = 100) private String taxName;
    @Column(precision = 8, scale = 2) private BigDecimal taxPercent;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC LANGUAGE
// =============================================================================

@Entity
@Table(name = "ec_languages",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_language",
                columnNames = "language_code"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcLanguage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20) private String languageCode;
    @Column(length = 100) private String languageName;
    @Column(length = 100) private String nativeName;

    @Builder.Default @Column(nullable = false) private boolean rtl             = false;
    @Builder.Default @Column(nullable = false) private boolean defaultLanguage  = false;
    @Builder.Default @Column(nullable = false) private boolean active           = true;
}

// =============================================================================
// EC DOCUMENT MAPPING  (ecommerce event → ERP DocumentType enum)
// debitAccount / creditAccount: optional GL overrides per mapping
// =============================================================================

@Entity
@Table(name = "ec_document_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_docmap",
                columnNames = {"organization_id", "ec_document_type"}),
        indexes = @Index(name = "idx_ec_docmap_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcDocumentMapping extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ecommerce lifecycle event (e.g. 'ORDER_CONFIRMED', 'PAYMENT_SUCCESS')
    @Column(length = 100)
    private String ecDocumentType;

    // ERP DocumentType enum name (e.g. 'SALES_INVOICE', 'RECEIPT_VOUCHER')
    @Column(length = 100)
    private String erpDocumentType;

    @Builder.Default @Column(nullable = false) private boolean autoCreate = true;

    // Optional GL account overrides for this specific mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_account_id")
    private ChartOfAccount debitAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id")
    private ChartOfAccount creditAccount;
}

// =============================================================================
// EC FEATURE FLAG  (per-org feature toggles)
// =============================================================================

@Entity
@Table(name = "ec_feature_flags",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_feature",
                columnNames = {"organization_id", "feature_name"}),
        indexes = @Index(name = "idx_ec_feature_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcFeatureFlag extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200) private String featureName;
    @Builder.Default @Column(nullable = false) private boolean enabled = false;
    @Column(length = 500) private String notes;
}

// =============================================================================
// EC API CONFIGURATION  (payment gateways, courier APIs)
// api_key / api_secret / password: encrypt at rest in production
// =============================================================================

@Entity
@Table(name = "ec_api_configurations",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_api_config",
                columnNames = {"organization_id", "api_name"}),
        indexes = @Index(name = "idx_ec_apiconfig_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcApiConfiguration extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100) private String apiName;
    @Column(length = 700) private String apiUrl;
    @Column(columnDefinition = "text") private String apiKey;     // ENCRYPT IN PRODUCTION
    @Column(columnDefinition = "text") private String apiSecret;  // ENCRYPT IN PRODUCTION
    @Column(length = 200) private String username;
    @Column(columnDefinition = "text") private String password;   // ENCRYPT IN PRODUCTION
    @Column(length = 700) private String webhookUrl;

    @Builder.Default @Column(nullable = false) private boolean sandboxMode = true;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC SCHEDULED JOB
// =============================================================================

@Entity
@Table(name = "ec_scheduled_jobs",
        indexes = @Index(name = "idx_ec_job_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcScheduledJob {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // organizationId stored as Long (not BaseEntity — system-level jobs may be org-agnostic)
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(length = 200) private String jobName;
    @Column(length = 100) private String cronExpression;
    private LocalDateTime lastRun;
    private LocalDateTime nextRun;

    @Builder.Default
    @Column(length = 20)
    private String lastResult = "PENDING";  // SUCCESS | FAILED | RUNNING

    @Column(columnDefinition = "text") private String errorMessage;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}
