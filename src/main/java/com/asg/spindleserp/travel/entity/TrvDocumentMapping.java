package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_document_mapping", uniqueConstraints = @UniqueConstraint(
        name = "uq_trv_docmap", columnNames = {"organization_id", "trv_document_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvDocumentMapping extends BaseEntity {

    @Builder.Default
    @Column(name = "auto_create", nullable = false)
    private Boolean autoCreate = false;

    @Column(name = "trv_document_type", nullable = false, length = 50)
    private String trvDocumentType;

    @Column(name = "erp_document_type", length = 50)
    private String erpDocumentType;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "debit_account_id")
    private Long debitAccountId;

    @Column(name = "credit_account_id")
    private Long creditAccountId;
}
