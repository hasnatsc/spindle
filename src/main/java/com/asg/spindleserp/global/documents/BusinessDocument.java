package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.approval.ApprovalStatus;
import com.asg.spindleserp.approval.DocumentType;
import com.asg.spindleserp.commercial.CommercialLc;
import com.asg.spindleserp.common.BaseAuditEntity;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.inventory.setup.ItemType;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_documents",
        uniqueConstraints = @UniqueConstraint(name = "uk_gbd_no", columnNames = {"organization_id", "document_no"}),
        indexes = {
                @Index(name = "idx_gbd_org", columnList = "organization_id"),
                @Index(name = "idx_gbd_type", columnList = "document_type"),
                @Index(name = "idx_gbd_status", columnList = "status"),
                @Index(name = "idx_gbd_party", columnList = "party_id"),
                @Index(name = "idx_gbd_parent", columnList = "parent_document_id"),
                @Index(name = "idx_gbd_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_gbd_date", columnList = "document_date"),
                @Index(name = "idx_gbd_type_status", columnList = "document_type,status"),
                @Index(name = "idx_gbd_org_type", columnList = "organization_id,document_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity ─────────────────────────────────────────────────────────────
    @Column(name = "document_no", nullable = false, unique = true, length = 100)
    private String documentNo;
    @Column(name = "document_no_manual", length = 100)
    private String documentNoManual;
    @Column(name = "reference_no", length = 100)
    private String referenceNo;
    @Column(name = "reference_doc_id")
    private Long referenceDocId;
    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;
    @Column(name = "validity_date")
    private LocalDate validityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    // ── Status ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BusinessDocumentStatus status = BusinessDocumentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String priority = "NORMAL"; // NORMAL|URGENT

    // ── Relationships ─────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private SubAccount party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_document_id")
    private BusinessDocument parentDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private CommercialLc lc;

    // ── Currency ─────────────────────────────────────────────────────────────
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";
    @Column(name = "exchange_rate", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    // ── Dates ────────────────────────────────────────────────────────────────
    @Column(name = "required_date")
    private LocalDate requiredDate;
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    // ── Amounts ──────────────────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "due_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    // ── Logistics ─────────────────────────────────────────────────────────────
    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;
    @Column(name = "contact_person", length = 100)
    private String contactPerson;
    @Column(name = "contact_number", length = 20)
    private String contactNumber;
    @Column(name = "vehicle_number", length = 100)
    private String vehicleNumber;
    @Column(name = "driver_name", length = 100)
    private String driverName;
    @Column(name = "challan_no", length = 100)
    private String challanNo;

    // ── Export / Import ───────────────────────────────────────────────────────
    @Column(name = "export_lc_number", length = 100)
    private String exportLcNumber;
    @Column(name = "bl_number", length = 100)
    private String blNumber;
    @Column(name = "vessel_name", length = 100)
    private String vesselName;
    @Column(name = "container_number", length = 100)
    private String containerNumber;
    @Column(length = 50)
    private String incoterms;
    @Column(name = "port_of_loading", length = 100)
    private String portOfLoading;
    @Column(name = "port_of_discharge", length = 100)
    private String portOfDischarge;

    // ── Text & Flags ─────────────────────────────────────────────────────────
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Builder.Default
    @Column(name = "stock_posted", nullable = false)
    private Boolean stockPosted = false;
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // ── Lines ─────────────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<BusinessDocumentLine> lines = new ArrayList<>();

    public void calculateTotals() {
        this.subtotalAmount = lines.stream()
                .map(l -> l.getLineAmount() != null ? l.getLineAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotalAmount
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);
        this.dueAmount = totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
}
