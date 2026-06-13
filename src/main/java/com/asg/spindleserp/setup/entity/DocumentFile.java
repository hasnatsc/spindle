package com.asg.spindleserp.setup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_document_file",
        indexes = @Index(name = "idx_docfile_ref", columnList = "document_type, reference_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String documentType;
    @Column(nullable = false)
    private Long referenceId;
    @Column(length = 255)
    private String fileName;
    @Column(length = 255)
    private String originalFileName;
    @Column(length = 100)
    private String fileType;
    @Column(length = 500)
    private String filePath;
    private Long fileSize;
    @Column(length = 200)
    private String documentCategory;
    @Column(length = 500)
    private String remarks;
    private LocalDateTime uploadedAt;
    @Column(length = 255)
    private String uploadedBy;
}
