package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "apr_voucher")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalVoucher extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_master_id", nullable = false, unique = true)
    private JournalEntryMaster journalEntryMaster;

    @Column(nullable = false)
    private Integer approvalLevel;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    @Column(length = 100) private String approverName;
    @Column(length = 100) private String approverRole;
    private LocalDate approvalDate;
    @Column(length = 1000) private String approvalRemarks;
}
