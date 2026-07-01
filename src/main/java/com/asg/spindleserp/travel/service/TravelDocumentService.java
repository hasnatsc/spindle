package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvDocumentDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * TravelDocumentService — generic file attachment CRUD used across the
 * Travel module: passenger passport scans, air ticket PDFs, visa scans,
 * hotel/tour confirmations, supplier invoices.
 *
 * Storage: local filesystem under a configured root directory
 * (spindle.upload.dir), one sub-folder per entityType/entityId. The DB row
 * is the source of truth for metadata; the file on disk is deleted whenever
 * its DB row is deleted, so the two never drift out of sync.
 */
public interface TravelDocumentService {

    TrvDocumentDTO upload(String entityType, Long entityId, String documentType,
                           MultipartFile file, String remarks);

    List<TrvDocumentDTO> list(String entityType, Long entityId);

    /** Returns the file as a streamable Resource plus enough metadata to set response headers. */
    DownloadPayload download(Long documentId);

    void delete(Long documentId);

    record DownloadPayload(Resource resource, String originalFileName, String contentType) {}
}
