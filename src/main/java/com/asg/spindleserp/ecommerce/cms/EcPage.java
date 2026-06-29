package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_pages",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_page_slug",
                columnNames = {"organization_id", "slug"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcPage extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 250) private String pageTitle;
    @Column(length = 250) private String slug;
    @Column(columnDefinition = "text") private String pageContent;
    @Column(length = 255)  private String seoTitle;
    @Column(length = 500)  private String seoKeywords;
    @Column(length = 1000) private String seoDescription;
    @Builder.Default @Column(nullable = false) private boolean published = true;
    @Builder.Default private Integer displayOrder = 0;
}


