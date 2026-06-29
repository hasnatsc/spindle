package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.global.entity.InventoryLot;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EcOrder — ecommerce sales order.
 *
 * Lifecycle: PENDING → CONFIRMED → (PROCESSING … DELIVERED) → COMPLETED
 *            or → CANCELLED / RETURNED / REFUNDED
 *
 * GL bridge (mirrors Production.journalEntry and DepreciationRun.journalEntry):
 *   journal_entry_id populated on CONFIRMED transition by EcOrderJournalService
 *   VoucherType = SALES_VOUCHER
 *   DR  Accounts Receivable (customer erp_sub_account_id or default AR)
 *   CR  Sales Revenue       (ec_gl_account_defaults.salesRevenueAccountId)
 *   CR  VAT Payable         (ec_gl_account_defaults.vatPayableAccountId)
 *
 * Document number: EcOrderService calls DocumentSequenceService
 *   → nextDocumentNumber(orgId, "EC-ORDER-{orgCode}", year)
 *   → format: EC-ORDER-ASG-25-000001
 */
@Entity
@Table(name = "ec_orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_order_no",
                columnNames = {"organization_id", "order_no"}),
        indexes = {
                @Index(name = "idx_ec_order_cust",    columnList = "customer_id"),
                @Index(name = "idx_ec_order_status",  columnList = "order_status"),
                @Index(name = "idx_ec_order_payment", columnList = "payment_status"),
                @Index(name = "idx_ec_order_ship",    columnList = "shipping_status"),
                @Index(name = "idx_ec_order_date",    columnList = "order_date"),
                @Index(name = "idx_ec_order_org",     columnList = "organization_id"),
                @Index(name = "idx_ec_order_gl",      columnList = "journal_entry_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    // Soft FK — cart is closed/archived after order; avoid cascade risk
    private Long cartId;

    // System-generated via DocumentSequenceService
    @Column(nullable = false, length = 50)
    private String orderNo;

    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderSource orderSource = OrderSource.WEB;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShippingStatus shippingStatus = ShippingStatus.PENDING;

    // ── Financial summary ─────────────────────────────────────────────────
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal subtotal       = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal productDiscount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal couponDiscount  = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal shippingCharge  = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal taxAmount       = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal grandTotal;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal paidAmount      = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal dueAmount       = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 10)
    private String currencyCode = "BDT";

    @Builder.Default
    @Column(precision = 18, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(columnDefinition = "text") private String customerNote;
    @Column(columnDefinition = "text") private String adminNote;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    // ── GL BRIDGE ─────────────────────────────────────────────────────────
    // Stored as @ManyToOne (same pattern as DepreciationRun / Production)
    // SALES_VOUCHER created by EcOrderJournalService on CONFIRMED transition
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ── Collections ───────────────────────────────────────────────────────
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcOrderItem> orderItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcOrderStatusHistory> statusHistory = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcOrderAddress> addresses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcOrderNote> notes = new ArrayList<>();

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum OrderSource   { WEB, ANDROID, IOS, POS, FACEBOOK, API }
    public enum PaymentStatus { UNPAID, PARTIAL, PAID, REFUNDED }
    public enum ShippingStatus { PENDING, PACKED, SHIPPED, DELIVERED, RETURNED }

    public enum OrderStatus {
        PENDING, CONFIRMED, PROCESSING, PACKING, READY_TO_SHIP,
        SHIPPED, OUT_FOR_DELIVERY, DELIVERED, COMPLETED,
        CANCELLED, RETURNED, REFUNDED
    }
}

// =============================================================================
// EC ORDER ITEM
// item_id MANDATORY: used for stock deduction and COGS calculation
// cost_price: point-in-time snapshot of Item.costPrice at sale
// unit_price: point-in-time snapshot of what customer was charged
// inventory_lot_id: set only when Item.hasLotTracking = true
// =============================================================================

@Entity
@Table(name = "ec_order_items",
        indexes = {
                @Index(name = "idx_ec_orderitem_order", columnList = "order_id"),
                @Index(name = "idx_ec_orderitem_prod",  columnList = "product_id"),
                @Index(name = "idx_ec_orderitem_item",  columnList = "item_id"),
                @Index(name = "idx_ec_orderitem_lot",   columnList = "inventory_lot_id"),
                @Index(name = "idx_ec_orderitem_wh",    columnList = "warehouse_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcProductVariant variant;

    // MANDATORY: direct link to ERP Item master
    // Service enforces: if variant != null → item must = variant.item
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Set on READY_TO_SHIP / SHIPPED transition
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    // Set when item.hasLotTracking = true (populated by EcInventoryService)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private InventoryLot inventoryLot;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    // SNAPSHOTS — never re-derive from Item master after order save
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;       // what customer paid

    @Column(precision = 18, scale = 2)
    private BigDecimal costPrice;       // Item.costPrice at time of sale (COGS basis)

    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal taxAmount      = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    // Computed at order close: (unitPrice - costPrice) * quantity
    @Column(precision = 18, scale = 2)
    private BigDecimal profitAmount;

    @Column(length = 500)
    private String remarks;
}

// =============================================================================
// EC ORDER STATUS HISTORY
// =============================================================================

@Entity
@Table(name = "ec_order_status_history",
        indexes = {
                @Index(name = "idx_ec_orderhist_order", columnList = "order_id"),
                @Index(name = "idx_ec_orderhist_time",  columnList = "changed_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcOrderStatusHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String changedBy;
    @Builder.Default private LocalDateTime changedAt = LocalDateTime.now();
    @Column(length = 50) private String ipAddress;
}

// =============================================================================
// EC ORDER ADDRESS  (point-in-time snapshot — NEVER use live customer address)
// =============================================================================

@Entity
@Table(name = "ec_order_addresses",
        indexes = @Index(name = "idx_ec_orderaddr_order", columnList = "order_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcOrderAddress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AddressType addressType;

    @Column(length = 200) private String fullName;
    @Column(length = 30)  private String phone;
    @Column(length = 200) private String email;
    @Column(length = 100) private String country;
    @Column(length = 100) private String division;
    @Column(length = 100) private String district;
    @Column(length = 100) private String upazila;
    @Column(length = 20)  private String postcode;
    @Column(length = 300) private String addressLine1;
    @Column(length = 300) private String addressLine2;
    @Column(length = 200) private String landmark;

    public enum AddressType { BILLING, SHIPPING }
}

// =============================================================================
// EC ORDER NOTE
// =============================================================================

@Entity
@Table(name = "ec_order_notes",
        indexes = @Index(name = "idx_ec_ordernote_order", columnList = "order_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcOrderNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NoteType noteType;

    @Column(columnDefinition = "text") private String note;
    @Column(length = 100) private String createdBy;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    public enum NoteType { CUSTOMER, ADMIN, SYSTEM }
}
