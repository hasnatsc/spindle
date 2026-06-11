package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stp_banks",
        uniqueConstraints = @UniqueConstraint(name = "uk_bank_org_code", columnNames = {"organization_id", "bank_code"}),
        indexes = @Index(name = "idx_bank_swift", columnList = "swift_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;
    @Column(name = "bank_name", nullable = false, length = 200)
    private String bankName;
    @Column(name = "bank_name_local", length = 200)
    private String bankNameLocal;
    @Column(name = "short_name", length = 50)
    private String shortName;
    @Column(name = "swift_code", length = 11)
    private String swiftCode;
    @Column(name = "head_office_address", length = 500)
    private String headOfficeAddress;
    @Column(name = "head_office_country", length = 100)
    @Builder.Default
    private String headOfficeCountry = "Bangladesh";

    @Builder.Default
    @Column(name = "supports_lc", nullable = false)
    private Boolean supportsLc = false;
    @Builder.Default
    @Column(name = "supports_import_lc", nullable = false)
    private Boolean supportsImportLc = false;
    @Builder.Default
    @Column(name = "supports_export_lc", nullable = false)
    private Boolean supportsExportLc = false;
    @Builder.Default
    @Column(name = "supports_btb_lc", nullable = false)
    private Boolean supportsBtbLc = false;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "bank", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BankAccount> bankAccounts = new ArrayList<>();
}
