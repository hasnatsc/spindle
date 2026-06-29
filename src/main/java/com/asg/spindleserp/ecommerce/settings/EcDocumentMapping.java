package com.asg.spindleserp.ecommerce.settings;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_document_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_docmap",
                columnNames = {"organization_id", "ec_document_type"}),
        indexes = @Index(name = "idx_ec_docmap_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcDocumentMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ecommerce lifecycle event (e.g. 'ORDER_CONFIRMED', 'PAYMENT_SUCCESS')
    @Column(length = 100)
    private String ecDocumentType;

    // ERP DocumentType enum name (e.g. 'SALES_INVOICE', 'RECEIPT_VOUCHER')
    @Column(length = 100)
    private String erpDocumentType;

    @Builder.Default
    @Column(nullable = false)
    private boolean autoCreate = true;

    // Optional GL account overrides for this specific mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_account_id")
    private ChartOfAccount debitAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id")
    private ChartOfAccount creditAccount;
}
