package com.asg.spindleserp.commercial.entity;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
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
                @Index(name = "idx_ci_lc", columnList = "lc_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private CommercialInvoice.InvoiceType invoiceType;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private CommercialInvoice.InvoiceStatus status;

    @Column(length = 10)
    private String currency;
    @Column(precision = 18, scale = 4)
    private BigDecimal exchangeRate;
    @Column(precision = 18, scale = 2)
    private BigDecimal totalAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal totalAmountBdt;
    @Column(length = 255)
    private String incoterms;
    @Column(length = 255)
    private String portOfLoading;
    @Column(length = 255)
    private String portOfDischarge;
    @Column(length = 255)
    private String vesselName;
    @Column(length = 255)
    private String blNumber;
    @Column(length = 255)
    private String containerNo;
    @Column(columnDefinition = "text")
    private String remarks;

    // stub FKs (resolved at app layer)
    private Long deliveryId;
    private Long grnId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommercialInvoiceItem> items = new ArrayList<>();

    public enum InvoiceType {EXPORT, IMPORT}

    public enum InvoiceStatus {DRAFT, FINALIZED, POSTED, CANCELLED}
}
