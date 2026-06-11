package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.dummy.EcoProductVariant;
import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "eco_order_lines",
        uniqueConstraints = @UniqueConstraint(name = "uk_oline_bdl", columnNames = {"business_document_line_id"}),
        indexes = {
                @Index(name = "idx_oline_order", columnList = "eco_order_id"),
                @Index(name = "idx_oline_prod", columnList = "product_id"),
                @Index(name = "idx_oline_var", columnList = "variant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoOrderLine implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_line_id", nullable = false, unique = true)
    private BusinessDocumentLine businessDocumentLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;
    @Column(name = "variant_sku", length = 100)
    private String variantSku;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variant_attributes", columnDefinition = "jsonb")
    private Map<String, String> variantAttributes;  // {"Colour":"White","Count":"30s"}
    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;
    @Builder.Default
    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;
    @Builder.Default
    @Column(name = "fulfilled_qty", precision = 12, scale = 3)
    private BigDecimal fulfilledQty = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "returned_qty", precision = 12, scale = 3)
    private BigDecimal returnedQty = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "refunded_amount", precision = 18, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "is_returnable", nullable = false)
    private Boolean isReturnable = true;
    @Column(name = "return_deadline")
    private LocalDate returnDeadline;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
