package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.dummy.EcoReturnItem;
import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table(name = "eco_returns",
        uniqueConstraints = @UniqueConstraint(name = "uk_return_doc", columnNames = {"business_document_id"}),
        indexes = {
                @Index(name = "idx_return_doc", columnList = "business_document_id"),
                @Index(name = "idx_return_order", columnList = "eco_order_id"),
                @Index(name = "idx_return_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoReturn extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false, unique = true)
    private BusinessDocument businessDocument;  // documentType = ONLINE_RETURN
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @Column(name = "return_reason", nullable = false, length = 30)
    private String returnReason;
    // WRONG_ITEM|DEFECTIVE|DAMAGED_IN_TRANSIT|NOT_AS_DESCRIBED|CHANGED_MIND|QUALITY_ISSUE|LATE_DELIVERY
    @Column(name = "reason_detail", columnDefinition = "TEXT")
    private String reasonDetail;
    @Column(name = "resolution_type", nullable = false, length = 20)
    @Builder.Default
    private String resolutionType = "REFUND"; // REFUND|REPLACEMENT|STORE_CREDIT|EXCHANGE
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "REQUESTED";
    // REQUESTED|APPROVED|REJECTED|PICKED_UP|RECEIVED|INSPECTED|RESOLVED
    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;
    @Column(name = "courier_tracking_no", length = 200)
    private String courierTrackingNo;
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    @Column(name = "received_by", length = 100)
    private String receivedBy;
    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;
    @Builder.Default
    @Column(name = "stock_posted", nullable = false)
    private Boolean stockPosted = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    private EcoRefund refund;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;
    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    @OneToMany(mappedBy = "ecoReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoReturnItem> items = new ArrayList<>();
}
