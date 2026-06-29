package com.asg.spindleserp.ecommerce.order;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.ecommerce.EcCustomer;
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
