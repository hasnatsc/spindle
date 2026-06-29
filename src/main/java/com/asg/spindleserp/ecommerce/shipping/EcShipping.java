package com.asg.spindleserp.ecommerce.shipping;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.order.EcOrder;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC SHIPPING METHOD MASTER
// =============================================================================

// =============================================================================
// EC SHIPPING ZONE
// =============================================================================

// =============================================================================
// EC SHIPMENT
// warehouse_id → org_warehouses (ERP dispatch warehouse for SALES_ISSUE movement)
// =============================================================================

@Entity
@Table(name = "ec_shipping",
        indexes = {
                @Index(name = "idx_ec_shipping_order",  columnList = "order_id"),
                @Index(name = "idx_ec_shipping_status", columnList = "shipping_status"),
                @Index(name = "idx_ec_shipping_wh",     columnList = "warehouse_id"),
                @Index(name = "idx_ec_shipping_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShipping extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    private EcShippingMethod shippingMethod;

    // ERP warehouse — stock deduction fires when status → SHIPPED
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(length = 50)  private String shipmentNo;
    @Column(length = 100) private String courierName;
    @Column(length = 150) private String trackingNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShippingStatus shippingStatus = ShippingStatus.PENDING;

    private LocalDateTime shippedDate;
    private LocalDate expectedDelivery;
    private LocalDateTime deliveredDate;

    @Column(precision = 18, scale = 2) private BigDecimal shippingCharge;
    @Column(precision = 12, scale = 3) private BigDecimal packageWeight;
    @Column(precision = 12, scale = 3) private BigDecimal packageLength;
    @Column(precision = 12, scale = 3) private BigDecimal packageWidth;
    @Column(precision = 12, scale = 3) private BigDecimal packageHeight;

    @Column(columnDefinition = "text") private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "shipping", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcShippingTracking> trackingEvents = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "shipping", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcShipmentPackage> packages = new ArrayList<>();

    public enum ShippingStatus {
        PENDING, PICKED, PACKED, SHIPPED, IN_TRANSIT,
        OUT_FOR_DELIVERY, DELIVERED, FAILED, RETURNED, CANCELLED
    }
}

// =============================================================================
// EC SHIPPING TRACKING EVENTS
// =============================================================================

// =============================================================================
// EC DELIVERY SLOT
// =============================================================================

// =============================================================================
// EC ORDER DELIVERY SLOT BOOKING
// =============================================================================

// =============================================================================
// EC SHIPMENT PACKAGE
// =============================================================================

// =============================================================================
// EC SHIPPING API LOG  (courier API audit — immutable JSONB)
// =============================================================================

