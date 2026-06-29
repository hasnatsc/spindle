package com.asg.spindleserp.ecommerce.marketing;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_loyalty_programs",
        indexes = @Index(name = "idx_ec_loyalty_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcLoyaltyProgram extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String programName;
    @Column(precision = 12, scale = 4)
    private BigDecimal earnRate;    // points per BDT spent
    @Column(precision = 12, scale = 4)
    private BigDecimal redeemRate;  // BDT per point
    private Integer minimumPoints;
    private Integer expiryDays;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
