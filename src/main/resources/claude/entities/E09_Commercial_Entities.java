// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E09  Commercial / LC                                      ║
// ║  Tables: com_commercial_invoice, com_commercial_invoice_item,            ║
// ║           com_document_terms, com_lc_document_mapping,                  ║
// ║           com_lc_settlement                                              ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: commercial/entity/CommercialInvoice.java ───────────────────────────
package com.hasnat.optimum.commercial.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "com_commercial_invoice",
    uniqueConstraints = @UniqueConstraint(name = "uq_ci_invoice_no", columnNames = "invoice_no"),
    indexes = {
        @Index(name = "idx_ci_org", columnList = "organization_id"),
        @Index(name = "idx_ci_lc",  columnList = "lc_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommercialInvoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private ChartOfAccountSub lc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private ChartOfAccountSub party;

    @Column(nullable = false, unique = true, length = 255)
    private String invoiceNo;

    private LocalDate invoiceDate;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(length = 10)   private String currency;
    @Column(precision = 18, scale = 4) private BigDecimal exchangeRate;
    @Column(precision = 18, scale = 2) private BigDecimal totalAmount;
    @Column(precision = 18, scale = 2) private BigDecimal totalAmountBdt;
    @Column(length = 255)  private String incoterms;
    @Column(length = 255)  private String portOfLoading;
    @Column(length = 255)  private String portOfDischarge;
    @Column(length = 255)  private String vesselName;
    @Column(length = 255)  private String blNumber;
    @Column(length = 255)  private String containerNo;
    @Column(columnDefinition = "text") private String remarks;

    // stub FKs (resolved at app layer)
    private Long deliveryId;
    private Long grnId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommercialInvoiceItem> items = new ArrayList<>();

    public enum InvoiceType   { EXPORT, IMPORT }
    public enum InvoiceStatus { DRAFT, FINALIZED, POSTED, CANCELLED }
}


// ── FILE: commercial/entity/CommercialInvoiceItem.java ───────────────────────
package com.hasnat.optimum.commercial.entity;

import com.hasnat.optimum.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "com_commercial_invoice_item",
    indexes = @Index(name = "idx_cii_invoice", columnList = "invoice_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommercialInvoiceItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CommercialInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity;
    @Column(nullable = false, precision = 18, scale = 4) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalAmount;
    @Column(length = 500) private String description;
    @Column(length = 20)  private String unit;
    private Long deliveryDetailId;   // stub FK
}


// ── FILE: commercial/entity/DocumentTerm.java ────────────────────────────────
package com.hasnat.optimum.commercial.entity;

import com.hasnat.optimum.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "com_document_terms",
    indexes = @Index(name = "idx_cdterm_doc", columnList = "document_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentTerm {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private BusinessDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private CommercialInvoice invoice;

    private Long globalTermsId;  // soft ref to stp_terms_master

    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "text")      private String description;
    private Integer sortOrder;
}


// ── FILE: commercial/entity/LcDocumentMapping.java ───────────────────────────
package com.hasnat.optimum.commercial.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "com_lc_document_mapping")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LcDocumentMapping {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private ChartOfAccountSub lc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private BusinessDocument document;

    @Column(precision = 18, scale = 2) private BigDecimal allocatedAmount;
    @Column(precision = 18, scale = 2) private BigDecimal utilizedAmount;
}


// ── FILE: commercial/entity/LcSettlement.java ────────────────────────────────
package com.hasnat.optimum.commercial.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "com_lc_settlement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LcSettlement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private ChartOfAccountSub lc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private BusinessDocument document;

    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING) @Column(length = 20) private SettlementType   settlementType;
    @Enumerated(EnumType.STRING) @Column(length = 20) private SettlementStatus status;

    @Column(precision = 18, scale = 4) private BigDecimal exchangeRate;
    @Column(precision = 18, scale = 2) private BigDecimal amountUsd;
    @Column(precision = 18, scale = 2) private BigDecimal amountBdt;
    @Column(precision = 18, scale = 2) private BigDecimal marginUsed;
    @Column(precision = 18, scale = 2) private BigDecimal charges;
    @Column(precision = 18, scale = 2) private BigDecimal commission;
    @Column(precision = 18, scale = 2) private BigDecimal interest;
    @Column(precision = 18, scale = 2) private BigDecimal loanAmount;

    public enum SettlementType   { SIGHT, USANCE, LOAN_ADJUSTMENT }
    public enum SettlementStatus { PENDING, PARTIAL, SETTLED, REVERSED }
}
