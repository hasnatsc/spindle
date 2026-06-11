package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.production.order.Opportunity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_eco_order_doc", columnNames = {"business_document_id"}),
        indexes = {
                @Index(name = "idx_order_doc", columnList = "business_document_id"),
                @Index(name = "idx_order_store", columnList = "store_id"),
                @Index(name = "idx_order_customer", columnList = "customer_id"),
                @Index(name = "idx_order_status", columnList = "order_status"),
                @Index(name = "idx_order_pay_status", columnList = "payment_status"),
                @Index(name = "idx_order_date", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoOrder extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Header stored in global_business_documents (documentType = ONLINE_ORDER)
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false, unique = true)
    private BusinessDocument businessDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcoCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private EcoCart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private EcoPaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    private EcoShippingMethod shippingMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private EcoCoupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_doc_id")
    private BusinessDocument salesInvoice;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String channel = "WEB"; // WEB|MOBILE|API|POS|WHATSAPP|PHONE

    @Column(name = "order_status", nullable = false, length = 30)
    @Builder.Default
    private String orderStatus = "PENDING";
    // PENDING|CONFIRMED|PROCESSING|PACKED|SHIPPED|OUT_FOR_DELIVERY|DELIVERED|CANCELLED|REFUNDED

    // Shipping address (snapshot)
    @Column(name = "shipping_address1", nullable = false, length = 300)
    private String shippingAddress1;
    @Column(name = "shipping_address2", length = 300)
    private String shippingAddress2;
    @Column(name = "shipping_city", nullable = false, length = 100)
    private String shippingCity;
    @Column(name = "shipping_district", length = 100)
    private String shippingDistrict;
    @Column(name = "shipping_state", length = 100)
    private String shippingState;
    @Column(name = "shipping_country", nullable = false, length = 2)
    @Builder.Default
    private String shippingCountry = "BD";
    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;
    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;
    @Column(name = "shipping_email", length = 150)
    private String shippingEmail;
    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    // Amounts
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "coupon_code", length = 50)
    private String couponCode;
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
    @Column(name = "cod_charge", nullable = false, precision = 18, scale = 2)
    private BigDecimal codCharge = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "refunded_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";

    @Column(name = "payment_method_name", length = 100)
    private String paymentMethodName;
    @Column(name = "shipping_method_name", length = 100)
    private String shippingMethodName;
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;
    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private String paymentStatus = "PENDING";
    // PENDING|PAID|PARTIALLY_PAID|FAILED|REFUNDED|PARTIALLY_REFUNDED|COD_COLLECTED

    @Column(name = "packed_at")
    private LocalDateTime packedAt;
    @Column(name = "packed_by", length = 100)
    private String packedBy;
    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;
    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "ecoOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoOrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "ecoOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<EcoOrderStatusHistory> statusHistory = new ArrayList<>();
}
