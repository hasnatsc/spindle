package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.organization.entity.Warehouse;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC SHIPPING METHOD MASTER
// =============================================================================

@Entity
@Table(name = "ec_shipping_methods",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_ship_method",
                columnNames = {"organization_id", "method_code"}),
        indexes = @Index(name = "idx_ec_shipmethod_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShippingMethod extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30) private String methodCode;
    @Column(length = 100) private String methodName;
    @Column(length = 100) private String courierName;
    private Integer estimatedDays;
    @Column(precision = 18, scale = 2) private BigDecimal baseCharge;
    @Column(precision = 18, scale = 2) private BigDecimal chargePerKg;

    @Builder.Default @Column(nullable = false) private boolean cashOnDelivery = false;
    @Builder.Default @Column(nullable = false) private boolean apiEnabled     = false;
    @Builder.Default @Column(nullable = false) private boolean active         = true;
}

// =============================================================================
// EC SHIPPING ZONE
// =============================================================================

@Entity
@Table(name = "ec_shipping_zones",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_ship_zone",
                columnNames = {"organization_id", "zone_code"}),
        indexes = @Index(name = "idx_ec_shipzone_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShippingZone extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)  private String zoneCode;
    @Column(length = 150) private String zoneName;
    @Builder.Default @Column(length = 100) private String country = "Bangladesh";
    @Column(length = 100) private String division;
    @Column(length = 100) private String district;
    @Column(precision = 18, scale = 2) private BigDecimal shippingCharge;
    @Column(precision = 18, scale = 2) private BigDecimal minimumOrderAmount;
    @Builder.Default @Column(nullable = false) private boolean freeShipping = false;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

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

@Entity
@Table(name = "ec_shipping_tracking",
        indexes = {
                @Index(name = "idx_ec_tracking_ship", columnList = "shipping_id"),
                @Index(name = "idx_ec_tracking_time", columnList = "event_time")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShippingTracking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_id", nullable = false)
    private EcShipping shipping;

    @Column(length = 50)  private String trackingStatus;
    @Column(length = 200) private String trackingLocation;
    private LocalDateTime eventTime;
    @Column(columnDefinition = "text") private String remarks;
}

// =============================================================================
// EC DELIVERY SLOT
// =============================================================================

@Entity
@Table(name = "ec_delivery_slots",
        indexes = @Index(name = "idx_ec_slot_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcDeliverySlot extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100) private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maximumOrders;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC ORDER DELIVERY SLOT BOOKING
// =============================================================================

@Entity
@Table(name = "ec_order_delivery_slots",
        indexes = {
                @Index(name = "idx_ec_orderslot_order", columnList = "order_id"),
                @Index(name = "idx_ec_orderslot_date",  columnList = "delivery_date")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcOrderDeliverySlot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_slot_id", nullable = false)
    private EcDeliverySlot deliverySlot;

    @Column(nullable = false) private LocalDate deliveryDate;
    @Builder.Default @Column(nullable = false) private boolean confirmed = false;
}

// =============================================================================
// EC SHIPMENT PACKAGE
// =============================================================================

@Entity
@Table(name = "ec_shipment_packages",
        indexes = @Index(name = "idx_ec_package_ship", columnList = "shipping_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShipmentPackage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_id", nullable = false)
    private EcShipping shipping;

    @Column(length = 50) private String packageNo;
    @Column(precision = 12, scale = 3) private BigDecimal weight;
    @Column(precision = 12, scale = 3) private BigDecimal length;
    @Column(precision = 12, scale = 3) private BigDecimal width;
    @Column(precision = 12, scale = 3) private BigDecimal height;
    private Integer itemCount;
    @Column(columnDefinition = "text") private String remarks;
}

// =============================================================================
// EC SHIPPING API LOG  (courier API audit — immutable JSONB)
// =============================================================================

@Entity
@Table(name = "ec_shipping_api_logs",
        indexes = {
                @Index(name = "idx_ec_shipapi_ship", columnList = "shipping_id"),
                @Index(name = "idx_ec_shipapi_time", columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcShippingApiLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_id")
    private EcShipping shipping;

    @Column(length = 100) private String apiName;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb") private String requestPayload;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb") private String responsePayload;

    @Column(length = 50)  private String responseCode;
    private Boolean success;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
