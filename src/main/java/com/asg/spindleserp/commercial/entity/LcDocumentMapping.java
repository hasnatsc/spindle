package com.asg.spindleserp.commercial.entity;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "com_lc_document_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LcDocumentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private ChartOfAccountSub lc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private BusinessDocument document;

    @Column(precision = 18, scale = 2)
    private BigDecimal allocatedAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal utilizedAmount;
}
