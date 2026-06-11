package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fa_asset_disposals",
        indexes = @Index(name = "idx_fadis_asset", columnList = "asset_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDisposal extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(name = "disposal_date", nullable = false)
    private LocalDate disposalDate;
    @Column(name = "disposal_type", nullable = false, length = 30)
    private String disposalType; // SALE|WRITE_OFF|TRANSFER|SCRAP

    @Builder.Default
    @Column(name = "disposal_value", precision = 18, scale = 2)
    private BigDecimal disposalValue = BigDecimal.ZERO;
    @Column(name = "book_value_at_disposal", nullable = false, precision = 18, scale = 2)
    private BigDecimal bookValueAtDisposal;
    @Column(name = "accumulated_dep_at_disp", nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepAtDisp;
    @Column(name = "gain_loss", precision = 18, scale = 2)
    private BigDecimal gainLoss;  // +ve = gain

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_dept_id")
    private Department transferToDept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_employee_id")
    private Employee transferToEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
