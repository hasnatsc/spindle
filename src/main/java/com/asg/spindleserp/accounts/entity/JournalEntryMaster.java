package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.common.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_journal_entry_master",
        indexes = {
                @Index(name = "idx_jem_org", columnList = "organization_id"),
                @Index(name = "idx_jem_date", columnList = "voucher_date"),
                @Index(name = "idx_jem_type", columnList = "voucher_type"),
                @Index(name = "idx_jem_posted", columnList = "is_posted"),
                @Index(name = "idx_jem_no", columnList = "voucher_no")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(length = 100)
    private String voucherNo;
    private LocalDate voucherDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VoucherType voucherType;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalDebit;
    @Column(precision = 18, scale = 2)
    private BigDecimal totalCredit;
    @Column(length = 1000)
    private String narration;
    @Column(length = 100)
    private String referenceNo;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPosted = false;
    @Column(length = 100)
    private String postedBy;
    private LocalDateTime postedAt;

    @Builder.Default
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();
}
