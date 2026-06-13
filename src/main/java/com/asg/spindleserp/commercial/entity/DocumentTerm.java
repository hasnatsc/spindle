package com.asg.spindleserp.commercial.entity;

import com.asg.spindleserp.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "com_document_terms",
        indexes = @Index(name = "idx_cdterm_doc", columnList = "document_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private BusinessDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private CommercialInvoice invoice;

    private Long globalTermsId;  // soft ref to stp_terms_master

    @Column(nullable = false, length = 200)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    private Integer sortOrder;
}
