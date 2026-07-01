package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * TrvGlAccountDefaults — singleton-per-organization upsert, no DataTable.
 * Mirrors ec_gl_account_defaults. Must be configured before any booking
 * can be confirmed (GL bridge validation reads this row).
 */
@Entity
@Table(name = "trv_gl_account_defaults", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_gldef_org", columnNames = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvGlAccountDefaults extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accounts_receivable_id")
    private Long accountsReceivableId;

    @Column(name = "travel_revenue_account_id")
    private Long travelRevenueAccountId;

    @Column(name = "cost_of_service_account_id")
    private Long costOfServiceAccountId;

    @Column(name = "supplier_payable_default_id")
    private Long supplierPayableDefaultId;
}
