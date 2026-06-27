package com.asg.spindleserp.commercial.entity;


import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "com_lc_settlement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LcSettlement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private ChartOfAccountSub lc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private BusinessDocument document;

    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING) @Column(length = 20) private SettlementType   settlementType;
    @Enumerated(EnumType.STRING) @Column(length = 20) private SettlementStatus status;

    @Column(precision = 18, scale = 4) private BigDecimal exchangeRate;
    @Column(precision = 18, scale = 2) private BigDecimal amountUsd;
    @Column(precision = 18, scale = 2) private BigDecimal amountBdt;
    @Column(precision = 18, scale = 2) private BigDecimal marginUsed;
    @Column(precision = 18, scale = 2) private BigDecimal charges;
    @Column(precision = 18, scale = 2) private BigDecimal commission;
    @Column(precision = 18, scale = 2) private BigDecimal interest;
    @Column(precision = 18, scale = 2) private BigDecimal loanAmount;

    public enum SettlementType   { SIGHT, USANCE, LOAN_ADJUSTMENT }
    public enum SettlementStatus { PENDING, PARTIAL, SETTLED, REVERSED }
}
