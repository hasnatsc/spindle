package com.asg.spindleserp.fixedassets.entity;

import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.hrm.entity.Employee;
import com.asg.spindleserp.organization.entity.Department;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_asset_disposals",
        indexes = {
                @Index(name = "idx_fad_org", columnList = "organization_id"),
                @Index(name = "idx_fad_asset", columnList = "asset_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDisposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_dept_id")
    private Department transferToDept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_employee_id")
    private Employee transferToEmployee;

    @Column(nullable = false)
    private LocalDate disposalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetDisposal.DisposalType disposalType;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal disposalValue = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal bookValueAtDisposal;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepAtDisposal;
    @Column(precision = 18, scale = 2)
    private BigDecimal gainLoss;
    @Column(length = 200)
    private String buyerName;
    @Column(columnDefinition = "text")
    private String reason;
    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum DisposalType {SALE, WRITE_OFF, TRANSFER, SCRAP, DONATION}
}
