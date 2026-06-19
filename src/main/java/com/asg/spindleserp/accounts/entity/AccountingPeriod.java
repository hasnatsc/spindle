package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "acc_periods",
        indexes = @Index(name = "idx_period_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String periodName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountingPeriod.PeriodType periodType;

    @Column(nullable = false)
    private int fiscalYear;
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isClosed = false;

    @Column(length = 100)
    private String closedBy;
    private LocalDate closedDate;

    public enum PeriodType {DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM}
}
