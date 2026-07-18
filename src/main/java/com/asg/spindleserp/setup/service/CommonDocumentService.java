package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.setup.dto.CommonDocumentDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * CommonDocumentService — generic file attachment CRUD shared by ALL modules.
 *
 * Storage: local filesystem under spindle.upload.dir, organised by
 * documentType/referenceId.  The DB row (stp_document_file) holds metadata;
 * the physical file is deleted when its row is deleted.
 *
 * Usage from any module controller:
 *   POST   /api/common/documents/upload?documentType=X&amp;referenceId=Y&amp;file=...
 *   GET    /api/common/documents/list?documentType=X&amp;referenceId=Y
 *   GET    /api/common/documents/download/{id}
 *   DELETE /api/common/documents/delete/{id}
 *
 * Or inject CommonDocumentService directly in module-specific controllers.
 */
public interface CommonDocumentService {

    CommonDocumentDTO upload(String documentType, Long referenceId,
                             String documentCategory, MultipartFile file, String remarks);

    List<CommonDocumentDTO> list(String documentType, Long referenceId);

    /** Returns the file as a streamable Resource plus response-header metadata. */
    DownloadPayload download(Long documentId);

    void delete(Long documentId);

    record DownloadPayload(Resource resource, String originalFileName, String contentType) {}
}
