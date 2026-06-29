package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_blog_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_blog_cat",
                columnNames = {"organization_id", "slug"}),
        indexes = {
                @Index(name = "idx_ec_blogcat_parent", columnList = "parent_category_id"),
                @Index(name = "idx_ec_blogcat_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcBlogCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private EcBlogCategory parentCategory;

    @Column(length = 200)
    private String categoryName;
    @Column(length = 250)
    private String slug;
    @Column(columnDefinition = "text")
    private String description;
    @Builder.Default
    private Integer displayOrder = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
