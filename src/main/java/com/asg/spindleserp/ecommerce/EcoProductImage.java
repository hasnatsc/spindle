package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.dummy.EcoProductVariant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_product_images",
        indexes = @Index(name = "idx_img_product", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoProductImage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
    @Column(name = "alt_text", length = 300)
    private String altText;
    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
