package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC BANNER
// =============================================================================

@Entity
@Table(name = "ec_banners",
        indexes = {
                @Index(name = "idx_ec_banner_org",    columnList = "organization_id"),
                @Index(name = "idx_ec_banner_type",   columnList = "banner_type"),
                @Index(name = "idx_ec_banner_active", columnList = "active")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcBanner extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)  private String bannerCode;
    @Column(nullable = false, length = 200) private String bannerName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BannerType bannerType;

    @Column(length = 250) private String title;
    @Column(length = 500) private String subTitle;
    @Column(columnDefinition = "text") private String description;
    @Column(length = 700) private String imageUrl;
    @Column(length = 700) private String mobileImageUrl;
    @Column(length = 100) private String buttonText;
    @Column(length = 500) private String buttonUrl;
    @Builder.Default @Column(nullable = false) private boolean openInNewTab = false;
    @Builder.Default private Integer displayOrder = 0;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Builder.Default @Column(nullable = false) private boolean active = true;

    public enum BannerType {
        HOME_SLIDER, HOME_TOP, HOME_MIDDLE, HOME_BOTTOM,
        CATEGORY, PRODUCT, POPUP, SIDEBAR
    }
}

// =============================================================================
// EC HOME SECTION
// =============================================================================

@Entity
@Table(name = "ec_home_sections",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_home_section",
                columnNames = {"organization_id", "section_code"}),
        indexes = @Index(name = "idx_ec_homesec_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcHomeSection extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)  private String sectionCode;
    @Column(length = 200) private String sectionName;
    @Column(length = 300) private String sectionTitle;
    @Column(length = 500) private String sectionSubtitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SectionType sectionType;

    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default private Integer maxProducts  = 12;
    @Builder.Default @Column(nullable = false) private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcHomeSectionProduct> sectionProducts = new ArrayList<>();

    public enum SectionType {
        FEATURED, NEW_ARRIVAL, BEST_SELLER, TOP_RATED, FLASH_SALE, TRENDING, CUSTOM
    }
}

// =============================================================================
// EC HOME SECTION PRODUCT
// =============================================================================

@Entity
@Table(name = "ec_home_section_products",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_home_prod",
                columnNames = {"section_id", "product_id"}),
        indexes = @Index(name = "idx_ec_homeprod_sec", columnList = "section_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcHomeSectionProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private EcHomeSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default private Integer displayOrder = 0;
}

// =============================================================================
// EC BLOG CATEGORY
// =============================================================================

@Entity
@Table(name = "ec_blog_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_blog_cat",
                columnNames = {"organization_id", "slug"}),
        indexes = {
                @Index(name = "idx_ec_blogcat_parent", columnList = "parent_category_id"),
                @Index(name = "idx_ec_blogcat_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcBlogCategory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private EcBlogCategory parentCategory;

    @Column(length = 200) private String categoryName;
    @Column(length = 250) private String slug;
    @Column(columnDefinition = "text") private String description;
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC BLOG POST
// authorUser: sec_users — ERP staff who write blog content
// =============================================================================

@Entity
@Table(name = "ec_blog_posts",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_blog_slug",
                columnNames = {"organization_id", "slug"}),
        indexes = {
                @Index(name = "idx_ec_blog_cat", columnList = "category_id"),
                @Index(name = "idx_ec_blog_pub", columnList = "published"),
                @Index(name = "idx_ec_blog_org", columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcBlogPost extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EcBlogCategory category;

    // ERP staff author
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    @Column(length = 300) private String title;
    @Column(length = 300) private String slug;
    @Column(columnDefinition = "text") private String shortDescription;
    @Column(columnDefinition = "text") private String content;
    @Column(length = 700) private String featuredImage;
    @Column(length = 255) private String seoTitle;
    @Column(length = 500) private String seoKeywords;
    @Column(length = 1000) private String seoDescription;
    private LocalDateTime publishDate;
    @Builder.Default @Column(nullable = false) private Long totalViews = 0L;
    @Builder.Default @Column(nullable = false) private boolean published = false;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC STATIC PAGE
// =============================================================================

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

// =============================================================================
// EC WEBSITE NAVIGATION MENU
// FIX: index renamed idx_ec_menu_parent (avoids collision with app_menus idx)
// =============================================================================

@Entity
@Table(name = "ec_menus",
        indexes = {
                @Index(name = "idx_ec_menu_parent", columnList = "parent_menu_id"),
                @Index(name = "idx_ec_menu_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcMenu extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_id")
    private EcMenu parentMenu;

    @Column(length = 200) private String menuName;
    @Column(length = 500) private String menuUrl;
    @Column(length = 100) private String menuIcon;
    @Builder.Default @Column(length = 30) private String target = "_self";
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC MEDIA LIBRARY
// uploadedBy: sec_users (ERP staff upload product/blog images)
// =============================================================================

@Entity
@Table(name = "ec_media_library",
        indexes = @Index(name = "idx_ec_media_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcMediaLibrary extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(length = 300) private String fileName;
    @Column(length = 300) private String originalName;
    @Column(length = 700) private String fileUrl;
    @Column(length = 700) private String thumbnailUrl;
    @Column(length = 100) private String mimeType;
    private Long fileSize;
    private Integer imageWidth;
    private Integer imageHeight;
    @Column(length = 300) private String altText;
    @Builder.Default private LocalDateTime uploadedAt = LocalDateTime.now();
}
