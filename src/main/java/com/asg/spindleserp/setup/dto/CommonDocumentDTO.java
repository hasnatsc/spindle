package com.asg.spindleserp.setup.dto;

import lombok.*;

/**
 * CommonDocumentDTO — returned by CommonDocumentService for any module's file attachment.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommonDocumentDTO {

    private Long id;

    private String documentType;
    private Long referenceId;
    private String documentCategory;

    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String remarks;

    private String uploadedBy;
    private String uploadedAt;

    /** Populated by the controller for the frontend download link — not persisted. */
    private String downloadUrl;
}
