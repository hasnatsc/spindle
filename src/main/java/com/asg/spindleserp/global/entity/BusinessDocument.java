package com.asg.spindleserp.global.entity;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.approval.entity.ApprovalRequest;
import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.organization.entity.Department;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_documents",
        indexes = {
                @Index(name = "idx_gbd_org", columnList = "organization_id"),
                @Index(name = "idx_gbd_type", columnList = "document_type"),
                @Index(name = "idx_gbd_status", columnList = "status"),
                @Index(name = "idx_gbd_party", columnList = "party_id"),
                @Index(name = "idx_gbd_parent", columnList = "parent_document_id"),
                @Index(name = "idx_gbd_wh", columnList = "warehouse_id"),
                @Index(name = "idx_gbd_date", columnList = "document_date"),
                @Index(name = "idx_gbd_no", columnList = "document_no"),
                @Index(name = "idx_gbd_deleted", columnList = "is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_document_id")
    private BusinessDocument parentDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private ChartOfAccountSub party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id")
    private Warehouse sourceWarehouse;

    @Column(nullable = false, unique = true, length = 100)
    private String documentNo;
    @Column(length = 100)
    private String documentNoManual;
    @Column(nullable = false)
    private LocalDate documentDate;

    // ★ UPDATED: includes PRODUCTION_ORDER, PRODUCTION_MATERIAL_ISSUE, FINISHED_GOODS_RECEIVE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType documentType;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(length = 30)
    private String approvalStatus;
    @Column(length = 20)
    private String currency;
    @Column(precision = 18, scale = 4)
    private BigDecimal exchangeRate;
    @Column(precision = 18, scale = 2)
    private BigDecimal subtotalAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal discountAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal taxAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal otherCharges;
    @Column(precision = 18, scale = 2)
    private BigDecimal totalAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal paidAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal dueAmount;

    @Builder.Default
    @Column(nullable = false)
    private boolean stockPosted = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean accountingPosted = false;

    @Column(length = 100)
    private String referenceNo;
    // Shipping fields
    @Column(length = 50)
    private String incoterms;
    @Column(length = 50)
    private String portOfLoading;
    @Column(length = 50)
    private String portOfDischarge;
    @Column(length = 100)
    private String vesselName;
    @Column(length = 100)
    private String blNumber;
    @Column(length = 100)
    private String containerNumber;
    // Delivery fields
    @Column(length = 100)
    private String challanNo;
    @Column(length = 100)
    private String vehicleNumber;
    @Column(length = 100)
    private String driverName;
    @Column(length = 500)
    private String deliveryAddress;
    private LocalDate deliveryDate;
    private LocalDate requiredDate;
    private LocalDate validityDate;
    @Column(length = 100)
    private String contactPerson;
    @Column(length = 20)
    private String contactNumber;
    @Column(columnDefinition = "text")
    private String termsAndConditions;
    @Column(columnDefinition = "text")
    private String remarks;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;
    private LocalDateTime deletedAt;
    @Column(length = 100)
    private String deletedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(length = 100)
    private String createdBy;
    @Column(length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder.Default
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessDocumentLine> lines = new ArrayList<>();
}
