package com.asg.spindleserp.ecommerce.eCommerceReturn;

import com.asg.spindleserp.ecommerce.order.EcOrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_return_items",
        indexes = @Index(name = "idx_ec_returnitem_ret", columnList = "return_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private EcReturn ecReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private EcOrderItem orderItem;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConditionStatus conditionStatus;

    @Column(precision = 12, scale = 3)
    private BigDecimal approvedQty;

    public enum ConditionStatus {GOOD, DAMAGED, USED, DEFECTIVE}
}
