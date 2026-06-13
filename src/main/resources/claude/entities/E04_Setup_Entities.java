// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E04  Setup / Reference Masters                            ║
// ║  Tables: stp_banks, stp_currencies, stp_document_sequences,              ║
// ║           stp_terms_master, stp_document_file, stp_location_*,           ║
// ║           com_hs_codes                                                   ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: setup/entity/Currency.java ─────────────────────────────────────────
package com.hasnat.optimum.setup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_currencies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Currency {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 10)
    private String symbol;

    @Builder.Default
    @Column(nullable = false)
    private int decimalPlaces = 2;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}


// ── FILE: setup/entity/Country.java ──────────────────────────────────────────
package com.hasnat.optimum.setup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stp_location_countries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Country {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(nullable = false, unique = true, length = 3)
    private String isoCode;

    @Column(nullable = false, length = 2)
    private String isoCode2;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150) private String nameNative;
    @Column(length = 10)  private String phoneCode;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
}


// ── FILE: setup/entity/Bank.java ─────────────────────────────────────────────
package com.hasnat.optimum.setup.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_banks",
    uniqueConstraints = @UniqueConstraint(name = "uq_bank_org_code",
        columnNames = {"organization_id", "bank_code"}),
    indexes = @Index(name = "idx_bank_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bank extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)
    private String bankCode;

    @Column(nullable = false, length = 200)
    private String bankName;

    @Column(length = 200) private String bankNameLocal;
    @Column(length = 50)  private String shortName;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String bankType = "COMMERCIAL";

    @Column(length = 30) private String bankCategory;
    @Column(length = 11) private String swiftCode;
    @Column(length = 20) private String centralBankCode;
    @Column(length = 9)  private String routingNumberPrefix;

    @Column(length = 500) private String headOfficeAddress;
    @Column(length = 100) private String headOfficeCity;
    @Column(length = 100) private String headOfficeCountry;
    @Column(length = 50)  private String headOfficePhone;
    @Column(length = 100) private String headOfficeEmail;
    @Column(length = 200) private String website;

    @Column(length = 200) private String correspondentBankName;
    @Column(length = 11)  private String correspondentSwiftCode;
    @Column(length = 50)  private String correspondentAccountNumber;

    @Builder.Default @Column(length = 20) private String rating = "UNRATED";

    @Builder.Default @Column(nullable = false) private boolean supportsLc             = false;
    @Builder.Default @Column(nullable = false) private boolean supportsImportLc        = false;
    @Builder.Default @Column(nullable = false) private boolean supportsExportLc        = false;
    @Builder.Default @Column(nullable = false) private boolean supportsBtbLc           = false;
    @Builder.Default @Column(nullable = false) private boolean supportsInlandLc        = false;
    @Builder.Default @Column(nullable = false) private boolean supportsOnlineBanking   = false;
    @Builder.Default @Column(nullable = false) private boolean isActive                = true;

    public enum BankType   { COMMERCIAL, STATE_OWNED, PRIVATE, FOREIGN, SPECIALIZED, ISLAMIC, DEVELOPMENT, COOPERATIVE }
    public enum BankRating { UNRATED, EXCELLENT, GOOD, AVERAGE, POOR, BLACKLISTED }
}


// ── FILE: setup/entity/DocumentSequence.java ─────────────────────────────────
package com.hasnat.optimum.setup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stp_document_sequences",
    uniqueConstraints = @UniqueConstraint(name = "uq_docseq_org_prefix_year",
        columnNames = {"organization_id", "prefix", "year_code"}),
    indexes = @Index(name = "idx_docseq_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentSequence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)
    private String prefix;

    @Column(nullable = false, length = 6)
    private String yearCode;

    @Builder.Default
    @Column(nullable = false)
    private int lastSeq = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


// ── FILE: setup/entity/TermsMaster.java ──────────────────────────────────────
package com.hasnat.optimum.setup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_terms_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TermsMaster {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "text")      private String description;
    @Column(nullable = false, length = 50)  private String documentType;

    @Builder.Default private boolean isActive  = true;
    @Builder.Default private boolean isDefault = false;
    private Integer sortOrder;
}


// ── FILE: setup/entity/DocumentFile.java ─────────────────────────────────────
package com.hasnat.optimum.setup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stp_document_file",
    indexes = @Index(name = "idx_docfile_ref", columnList = "document_type, reference_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentFile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)  private String documentType;
    @Column(nullable = false)               private Long   referenceId;
    @Column(length = 255) private String fileName;
    @Column(length = 255) private String originalFileName;
    @Column(length = 100) private String fileType;
    @Column(length = 500) private String filePath;
    private Long fileSize;
    @Column(length = 200) private String documentCategory;
    @Column(length = 500) private String remarks;
    private LocalDateTime uploadedAt;
    @Column(length = 255) private String uploadedBy;
}


// ── FILE: commercial/entity/HsCode.java ──────────────────────────────────────
package com.hasnat.optimum.commercial.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "com_hs_codes",
    uniqueConstraints = @UniqueConstraint(name = "uq_hs_org_code",
        columnNames = {"organization_id", "hs_code"}),
    indexes = @Index(name = "idx_hscode_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HsCode extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)
    private String hsCode;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 200) private String shortDescription;

    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private HsType hsType = HsType.BOTH;

    @Column(precision = 6, scale = 2) private BigDecimal vatPercent;
    @Column(precision = 6, scale = 2) private BigDecimal customsDutyPercent;
    @Column(precision = 6, scale = 2) private BigDecimal supplementaryDutyPercent;
    @Column(precision = 6, scale = 2) private BigDecimal aitPercent;

    @Builder.Default @Column(nullable = false) private boolean isActive            = true;
    @Builder.Default @Column(nullable = false) private boolean isBondedAllowed     = false;
    @Builder.Default @Column(nullable = false) private boolean requiresImportPermit = false;
    @Builder.Default @Column(nullable = false) private boolean requiresExportPermit = false;

    public enum HsType { EXPORT, IMPORT, BOTH }
}
