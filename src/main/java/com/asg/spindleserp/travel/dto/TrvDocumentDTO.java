package com.asg.spindleserp.travel.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvDocumentDTO {

    private Long id;

    private String entityType;
    private Long entityId;

    @Builder.Default
    private String documentType = "OTHER";

    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private String remarks;

    private String uploadedBy;
    private String uploadedAt;

    /** Populated by the controller for the frontend download link — not persisted. */
    private String downloadUrl;
}
