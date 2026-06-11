package com.asg.spindleserp.stp;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_hs_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsCode extends BaseReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(length = 500)
    private String description;
    @Column(name = "duty_rate", precision = 5, scale = 2)
    private BigDecimal dutyRate;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
