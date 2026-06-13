package com.asg.spindleserp.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * FIX: implements Serializable
 *
 * Permission is the leaf node of the security object graph:
 *   User → Role → Permission
 *
 * All three must implement Serializable for Spring Session JDBC
 * to successfully serialize the SecurityContext into the database.
 */
@Entity
@Table(name = "sec_permissions",
    indexes = {
        @Index(name = "idx_perm_name",   columnList = "name"),
        @Index(name = "idx_perm_module", columnList = "module")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission implements Serializable {   // ✅ FIX

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String urlPattern;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 50)
    private String category;

    @Column(length = 80)
    private String module;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Module {
        CORE_SECURITY, HRM, SALES_CUSTOMER_OPERATIONS, PURCHASE_SUPPLIER,
        INVENTORY_WAREHOUSE, FINANCE_ACCOUNTS, PRODUCTION, PRODUCT_CATALOG_ECOMMERCE,
        POS, CRM, COMMUNICATION_NOTIFICATION, COMMERCIAL, REPORTS_ANALYTICS,
        BUDGET, FIXED_ASSETS
    }
}
