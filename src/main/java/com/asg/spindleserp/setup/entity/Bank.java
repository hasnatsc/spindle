package com.asg.spindleserp.setup.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_banks",
        uniqueConstraints = @UniqueConstraint(name = "uq_bank_org_code",
                columnNames = {"organization_id", "bank_code"}),
        indexes = @Index(name = "idx_bank_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)
    private String bankCode;

    @Column(nullable = false, length = 200)
    private String bankName;

    @Column(length = 200)
    private String bankNameLocal;
    @Column(length = 50)
    private String shortName;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String bankType = "COMMERCIAL";

    @Column(length = 30)
    private String bankCategory;
    @Column(length = 11)
    private String swiftCode;
    @Column(length = 20)
    private String centralBankCode;
    @Column(length = 9)
    private String routingNumberPrefix;

    @Column(length = 500)
    private String headOfficeAddress;
    @Column(length = 100)
    private String headOfficeCity;
    @Column(length = 100)
    private String headOfficeCountry;
    @Column(length = 50)
    private String headOfficePhone;
    @Column(length = 100)
    private String headOfficeEmail;
    @Column(length = 200)
    private String website;

    @Column(length = 200)
    private String correspondentBankName;
    @Column(length = 11)
    private String correspondentSwiftCode;
    @Column(length = 50)
    private String correspondentAccountNumber;

    @Builder.Default
    @Column(length = 20)
    private String rating = "UNRATED";

    @Builder.Default
    @Column(nullable = false)
    private boolean supportsLc = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean supportsImportLc = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean supportsExportLc = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean supportsBtbLc = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean supportsInlandLc = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean supportsOnlineBanking = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum BankType {COMMERCIAL, STATE_OWNED, PRIVATE, FOREIGN, SPECIALIZED, ISLAMIC, DEVELOPMENT, COOPERATIVE}

    public enum BankRating {UNRATED, EXCELLENT, GOOD, AVERAGE, POOR, BLACKLISTED}
}
