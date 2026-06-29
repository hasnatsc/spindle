package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_banners",
        indexes = {
                @Index(name = "idx_ec_banner_org", columnList = "organization_id"),
                @Index(name = "idx_ec_banner_type", columnList = "banner_type"),
                @Index(name = "idx_ec_banner_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcBanner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String bannerCode;
    @Column(nullable = false, length = 200)
    private String bannerName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EcBanner.BannerType bannerType;

    @Column(length = 250)
    private String title;
    @Column(length = 500)
    private String subTitle;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 700)
    private String imageUrl;
    @Column(length = 700)
    private String mobileImageUrl;
    @Column(length = 100)
    private String buttonText;
    @Column(length = 500)
    private String buttonUrl;
    @Builder.Default
    @Column(nullable = false)
    private boolean openInNewTab = false;
    @Builder.Default
    private Integer displayOrder = 0;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public enum BannerType {
        HOME_SLIDER, HOME_TOP, HOME_MIDDLE, HOME_BOTTOM,
        CATEGORY, PRODUCT, POPUP, SIDEBAR
    }
}
