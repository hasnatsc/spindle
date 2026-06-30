package com.asg.spindleserp.ecommerce.analytics.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_dashboard_kpis",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_kpi",
                columnNames = {"organization_id", "kpi_date"}),
        indexes = {
                @Index(name = "idx_ec_kpi_org", columnList = "organization_id"),
                @Index(name = "idx_ec_kpi_date", columnList = "kpi_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcDashboardKpi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate kpiDate;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalSales;
    private Integer totalOrders;
    private Integer totalCustomers;
    private Integer newCustomers;
    @Column(precision = 18, scale = 2)
    private BigDecimal averageOrderValue;
    @Column(precision = 8, scale = 2)
    private BigDecimal conversionRate;
    @Column(precision = 8, scale = 2)
    private BigDecimal abandonedCartRate;
    @Column(precision = 8, scale = 2)
    private BigDecimal returnRate;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
