package com.asg.spindleserp.setup.controller;

import com.asg.spindleserp.setup.dto.CommonDocumentDTO;
import com.asg.spindleserp.setup.service.CommonDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommonDocumentController — generic file attachment REST endpoints
 * usable by ANY module (purchase, sales, CRM, HRM, commercial, budget, etc.).
 *
 * The documentType parameter acts as a namespace — use a logical name like
 * "PURCHASE_ORDER", "SALES_INVOICE", "CRM_LEAD", "HRM_EMPLOYEE", etc.
 *
 * Endpoints:
 *   POST   /api/common/documents/upload
 *   GET    /api/common/documents/list
 *   GET    /api/common/documents/download/{id}
 *   DELETE /api/common/documents/delete/{id}
 *
 * Frontend usage (jQuery example):
 *   var formData = new FormData();
 *   formData.append('file', fileInput.files[0]);
 *   formData.append('documentType', 'PURCHASE_ORDER');
 *   formData.append('referenceId', 42);
 *   $.ajax({ url: '/api/common/documents/upload', method: 'POST',
 *            data: formData, processData: false, contentType: false });
 */
@Slf4j
@RestController
@RequestMapping("/api/common/documents")
@RequiredArgsConstructor
public class CommonDocumentController {

    private final CommonDocumentService documentService;

    // =========================================================================
    // UPLOAD
    // =========================================================================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestParam String documentType,
            @RequestParam Long referenceId,
            @RequestParam(required = false) String documentCategory,
            @RequestParam(required = false) String remarks,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> res = new HashMap<>();
        try {
            CommonDocumentDTO saved = documentService.upload(
                    documentType, referenceId, documentCategory, file, remarks);
            res.put("success", true);
            res.put("document", saved);
            res.put("message", "\"" + saved.getOriginalFileName() + "\" uploaded.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam String documentType,
            @RequestParam Long referenceId) {

        Map<String, Object> res = new HashMap<>();
        try {
            List<CommonDocumentDTO> docs = documentService.list(documentType, referenceId);
            res.put("success", true);
            res.put("data", docs);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // =========================================================================
    // DOWNLOAD (forces download via Content-Disposition: attachment)
    // =========================================================================

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        CommonDocumentService.DownloadPayload payload = documentService.download(id);
        String encodedName = URLEncoder.encode(payload.originalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(payload.contentType() != null
                        ? MediaType.parseMediaType(payload.contentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(payload.resource());
    }

    // =========================================================================
    // SHOW / PREVIEW (inline display in browser — images, PDFs)
    // =========================================================================

    @GetMapping("/show/{id}")
    public ResponseEntity<Resource> show(@PathVariable Long id) {
        CommonDocumentService.DownloadPayload payload = documentService.download(id);
        String encodedName = URLEncoder.encode(payload.originalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        // Show inline — browser handles images, PDFs natively
        return ResponseEntity.ok()
                .contentType(payload.contentType() != null
                        ? MediaType.parseMediaType(payload.contentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedName)
                .body(payload.resource());
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            documentService.delete(id);
            res.put("success", true);
            res.put("message", "Document deleted.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }
}
