package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_media_library",
        indexes = @Index(name = "idx_ec_media_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcMediaLibrary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(length = 300)
    private String fileName;
    @Column(length = 300)
    private String originalName;
    @Column(length = 700)
    private String fileUrl;
    @Column(length = 700)
    private String thumbnailUrl;
    @Column(length = 100)
    private String mimeType;
    private Long fileSize;
    private Integer imageWidth;
    private Integer imageHeight;
    @Column(length = 300)
    private String altText;
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
