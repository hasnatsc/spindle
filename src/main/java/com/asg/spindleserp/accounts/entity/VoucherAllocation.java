package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * VoucherAllocation — records a settlement between two vouchers.
 *
 * source_voucher: the invoice / bill / credit-note being settled
 * paying_voucher: the payment / receipt / contra voucher doing the settling
 * allocated_amount: the amount being applied in this allocation
 *
 * Due Amount = source_voucher.total_amount - SUM(allocations.allocated_amount)
 */
@Entity
@Table(name = "acc_voucher_allocations",
        uniqueConstraints = @UniqueConstraint(name = "uq_alloc_src_pay",
                columnNames = {"source_voucher_id", "paying_voucher_id"}),
        indexes = {
                @Index(name = "idx_va_source", columnList = "source_voucher_id"),
                @Index(name = "idx_va_paying", columnList = "paying_voucher_id"),
                @Index(name = "idx_va_org",    columnList = "organization_id"),
                @Index(name = "idx_va_date",   columnList = "allocation_date"),
                @Index(name = "idx_va_party",  columnList = "source_party_id,source_party_type")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherAllocation extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_voucher_id", nullable = false)
    private JournalEntryMaster sourceVoucher;

    @Column(length = 100) private String sourceVoucherNo;
    @Column(length = 30)  private String sourceVoucherType;

    /** Sub-account being settled (supplier / customer / employee) */
    private Long   sourcePartyId;
    @Column(length = 20) private String sourcePartyType;  // SUPPLIER | CUSTOMER | EMPLOYEE

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paying_voucher_id", nullable = false)
    private JournalEntryMaster payingVoucher;

    @Column(length = 100) private String payingVoucherNo;
    @Column(length = 30)  private String payingVoucherType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal allocatedAmount;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal writeOffAmount = BigDecimal.ZERO;

    @Builder.Default
    private LocalDate allocationDate = LocalDate.now();

    @Column(length = 500) private String narration;
}
