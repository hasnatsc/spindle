package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_shipments",
        uniqueConstraints = @UniqueConstraint(name = "uk_shipment_doc", columnNames = {"business_document_id"}),
        indexes = {
                @Index(name = "idx_ship_doc", columnList = "business_document_id"),
                @Index(name = "idx_ship_order", columnList = "eco_order_id"),
                @Index(name = "idx_ship_status", columnList = "status"),
                @Index(name = "idx_ship_tracking", columnList = "tracking_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoShipment extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Header in global_business_documents (documentType = ONLINE_SHIPMENT)
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false, unique = true)
    private BusinessDocument businessDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    private EcoShippingMethod shippingMethod;

    @Column(name = "tracking_number", length = 200)
    private String trackingNumber;
    @Column(length = 100)
    private String carrier;
    @Column(name = "courier_order_id", length = 200)
    private String courierOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "courier_response", columnDefinition = "jsonb")
    private Map<String, Object> courierResponse;

    @Column(name = "shipping_label_url", length = 500)
    private String shippingLabelUrl;
    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";
    // PENDING|PACKED|DISPATCHED|IN_TRANSIT|OUT_FOR_DELIVERY|DELIVERED|FAILED|RETURNED

    @Column(name = "packed_at")
    private LocalDateTime packedAt;
    @Column(name = "packed_by", length = 100)
    private String packedBy;
    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;
    @Column(name = "dispatched_by", length = 100)
    private String dispatchedBy;
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    @Column(name = "delivery_confirmed_by", length = 100)
    private String deliveryConfirmedBy;

    @Column(name = "actual_weight_kg", precision = 8, scale = 3)
    private BigDecimal actualWeightKg;
    @Column(name = "shipping_cost", precision = 18, scale = 2)
    private BigDecimal shippingCost;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Builder.Default
    @Column(name = "stock_posted", nullable = false)
    private Boolean stockPosted = false;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoShipmentItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventTime ASC")
    @Builder.Default
    private List<EcoShipmentTracking> trackingEvents = new ArrayList<>();
}
