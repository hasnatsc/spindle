package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_url_redirects",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_redirect",
                columnNames = {"organization_id", "source_url"}),
        indexes = @Index(name = "idx_ec_redirect_src", columnList = "source_url"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcUrlRedirect extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 700) private String sourceUrl;
    @Column(nullable = false, length = 700) private String destinationUrl;
    @Column(nullable = false) private Integer redirectType;   // 301, 302, 307, 308
    @Builder.Default @Column(nullable = false) private boolean active = true;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}

