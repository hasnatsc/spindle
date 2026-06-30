package com.asg.spindleserp.ecommerce.analytics.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ec_seo_metadata",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_seo",
                columnNames = {"organization_id", "entity_type", "entity_id"}),
        indexes = {
                @Index(name = "idx_ec_seo_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_ec_seo_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcSeoMetadata extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EcSeoMetadata.EntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(length = 255)
    private String metaTitle;
    @Column(length = 1000)
    private String metaKeywords;
    @Column(length = 2000)
    private String metaDescription;
    @Column(length = 500)
    private String canonicalUrl;
    @Column(length = 100)
    private String robots;

    // JSON-LD structured data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String schemaJson;

    // Open Graph
    @Column(length = 255)
    private String ogTitle;
    @Column(length = 1000)
    private String ogDescription;
    @Column(length = 700)
    private String ogImage;

    // Twitter Card
    @Column(length = 255)
    private String twitterTitle;
    @Column(length = 1000)
    private String twitterDescription;
    @Column(length = 700)
    private String twitterImage;

    public enum EntityType {PRODUCT, CATEGORY, BLOG, PAGE, BRAND}
}
