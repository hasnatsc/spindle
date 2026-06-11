package com.asg.spindleserp.stp;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

/**
 * DocumentFile — polymorphic file attachment.
 * document_type + reference_id point to any entity.
 */
@Entity
@Table(name = "stp_document_files",
        indexes = @Index(name = "idx_docfile_ref", columnList = "document_type,reference_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentFile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;
    @Column(name = "file_name", length = 255)
    private String fileName;
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;
    @Column(name = "file_type", length = 50)
    private String fileType;     // PDF|JPG|PNG|XLSX
    @Column(name = "file_path", length = 1000)
    private String filePath;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "document_category", length = 200)
    private String documentCategory;
    @Column(length = 500)
    private String remarks;
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
