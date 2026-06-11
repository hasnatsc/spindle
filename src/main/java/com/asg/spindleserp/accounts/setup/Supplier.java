package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "acc_suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_org_code", columnNames = {"organization_id", "supplier_code"}))
@DiscriminatorValue("SUPPLIER")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Supplier extends SubAccount {

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;
    @Column(name = "lead_time_days")
    private Integer leadTimeDays;
    @Column(columnDefinition = "TEXT")
    private String certifications;
    @Builder.Default
    @Column(name = "is_import_supplier")
    private Boolean isImportSupplier = false;
    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency;
}
