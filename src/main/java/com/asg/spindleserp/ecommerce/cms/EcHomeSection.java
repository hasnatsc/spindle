package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ec_home_sections",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_home_section",
                columnNames = {"organization_id", "section_code"}),
        indexes = @Index(name = "idx_ec_homesec_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcHomeSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String sectionCode;
    @Column(length = 200)
    private String sectionName;
    @Column(length = 300)
    private String sectionTitle;
    @Column(length = 500)
    private String sectionSubtitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EcHomeSection.SectionType sectionType;

    @Builder.Default
    private Integer displayOrder = 0;
    @Builder.Default
    private Integer maxProducts = 12;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcHomeSectionProduct> sectionProducts = new ArrayList<>();

    public enum SectionType {
        FEATURED, NEW_ARRIVAL, BEST_SELLER, TOP_RATED, FLASH_SALE, TRENDING, CUSTOM
    }
}
