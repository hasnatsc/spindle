package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_auto_journal_templates",
        uniqueConstraints = @UniqueConstraint(name = "uq_ajt_org_code",
                columnNames = {"organization_id", "template_code"}),
        indexes = @Index(name = "idx_ajt_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoJournalTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_policy_id")
    private AccountsPolicy accountsPolicy;

    @Column(nullable = false, length = 30)
    private String templateCode;
    @Column(nullable = false, length = 200)
    private String templateName;
    @Column(nullable = false, length = 30)
    private String moduleType;
    @Column(nullable = false, length = 50)
    private String transactionType;
    @Column(length = 30)
    private String voucherType;
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String triggerMode = "MANUAL";
    @Column(length = 50)
    private String triggerEvent;
    @Column(length = 500)
    private String narrationTemplate;
    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean autoPost = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean validateBalance = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isSystem = false;

    private LocalDateTime lastUsedAt;
    @Builder.Default
    @Column(nullable = false)
    private int usageCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "autoJournalTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AutoJournalTemplateLine> lines = new ArrayList<>();
}
