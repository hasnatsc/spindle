package com.asg.spindleserp.setup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stp_document_sequences",
        uniqueConstraints = @UniqueConstraint(name = "uq_docseq_org_prefix_year",
                columnNames = {"organization_id", "prefix", "year_code"}),
        indexes = @Index(name = "idx_docseq_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)
    private String prefix;

    @Column(nullable = false, length = 6)
    private String yearCode;

    @Builder.Default
    @Column(nullable = false)
    private int lastSeq = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
