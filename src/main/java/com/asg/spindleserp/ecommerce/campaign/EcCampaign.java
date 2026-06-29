package com.asg.spindleserp.ecommerce.campaign;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ec_campaigns",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_campaign",
                columnNames = {"organization_id", "campaign_code"}),
        indexes = {
                @Index(name = "idx_ec_campaign_org", columnList = "organization_id"),
                @Index(name = "idx_ec_campaign_dates", columnList = "start_date,end_date"),
                @Index(name = "idx_ec_campaign_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String campaignCode;

    @Column(length = 200)
    private String campaignName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CampaignType campaignType;

    @Column(length = 700)
    private String bannerImage;

    @Column(columnDefinition = "text")
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcCampaignProduct> campaignProducts = new ArrayList<>();

    public enum CampaignType {
        FLASH_SALE, DISCOUNT, BUY_X_GET_Y, FREE_SHIPPING, SEASONAL, CLEARANCE
    }
}
