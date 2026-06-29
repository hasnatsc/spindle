package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_blog_posts",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_blog_slug",
                columnNames = {"organization_id", "slug"}),
        indexes = {
                @Index(name = "idx_ec_blog_cat", columnList = "category_id"),
                @Index(name = "idx_ec_blog_pub", columnList = "published"),
                @Index(name = "idx_ec_blog_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcBlogPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EcBlogCategory category;

    // ERP staff author
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    @Column(length = 300)
    private String title;
    @Column(length = 300)
    private String slug;
    @Column(columnDefinition = "text")
    private String shortDescription;
    @Column(columnDefinition = "text")
    private String content;
    @Column(length = 700)
    private String featuredImage;
    @Column(length = 255)
    private String seoTitle;
    @Column(length = 500)
    private String seoKeywords;
    @Column(length = 1000)
    private String seoDescription;
    private LocalDateTime publishDate;
    @Builder.Default
    @Column(nullable = false)
    private Long totalViews = 0L;
    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
