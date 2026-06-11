package com.asg.spindleserp.commercial;

import com.asg.spindleserp.accounts.setup.Bank;
import com.asg.spindleserp.accounts.setup.BankAccount;
import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "cmr_letters_of_credit",
        uniqueConstraints = @UniqueConstraint(name = "uk_lc_org_no", columnNames = {"organization_id", "lc_number"}),
        indexes = {
                @Index(name = "idx_lc_org", columnList = "organization_id"),
                @Index(name = "idx_lc_type", columnList = "lc_type"),
                @Index(name = "idx_lc_expiry", columnList = "expiry_date"),
                @Index(name = "idx_lc_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialLc extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuing_bank_id")
    private Bank issuingBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advising_bank_id")
    private Bank advisingBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private SubAccount beneficiary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuing_bank_account_id")
    private BankAccount issuingBankAccount;

    @Column(name = "lc_number", nullable = false, length = 100)
    private String lcNumber;
    @Column(name = "lc_type", nullable = false, length = 30)
    private String lcType; // EXPORT|IMPORT|BACK_TO_BACK|INLAND

    @Column(name = "lc_amount", precision = 18, scale = 2)
    private BigDecimal lcAmount;
    @Column(length = 3)
    private String currency;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "last_shipment_date")
    private LocalDate lastShipmentDate;
    @Column(name = "presentation_period")
    private Integer presentationPeriod;

    @Column(name = "partial_shipment", length = 20)
    @Builder.Default
    private String partialShipment = "NOT_ALLOWED";
    @Column(name = "transhipment", length = 20)
    @Builder.Default
    private String transhipment = "NOT_ALLOWED";
    @Column(name = "port_of_loading", length = 100)
    private String portOfLoading;
    @Column(name = "port_of_discharge", length = 100)
    private String portOfDischarge;
    @Column(length = 50)
    private String incoterms;

    @Column(name = "tolerance_plus", precision = 4, scale = 2)
    private BigDecimal tolerancePlus;
    @Column(name = "tolerance_minus", precision = 4, scale = 2)
    private BigDecimal toleranceMinus;
    @Column(name = "description_of_goods", columnDefinition = "TEXT")
    private String descriptionOfGoods;
    @Column(name = "special_conditions", columnDefinition = "TEXT")
    private String specialConditions;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|ACTIVE|AMENDED|UTILIZED|EXPIRED|CANCELLED

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
