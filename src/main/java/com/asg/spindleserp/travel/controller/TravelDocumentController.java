package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.TrvDocumentDTO;
import com.asg.spindleserp.travel.service.TravelDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * TravelDocumentController — generic file attachment endpoints reused across
 * the Travel module (passenger passport scans, air ticket PDFs, visa scans,
 * hotel/tour confirmations, supplier invoices).
 *
 * No standalone page — this is consumed as a modal/panel embedded in other
 * pages via the shared trvOpenDocuments(entityType, entityId, label) JS helper.
 *
 * REST:
 *   GET    /travel/documents/list?entityType=&entityId=
 *   POST   /travel/documents/upload   (multipart: entityType, entityId, documentType, remarks, file)
 *   GET    /travel/documents/download/{id}
 *   DELETE /travel/documents/delete/{id}
 */
@Slf4j
@RestController
@RequestMapping("/travel/documents")
@RequiredArgsConstructor
public class TravelDocumentController {

    private final TravelDocumentService documentService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam String entityType, @RequestParam Long entityId) {
        Map<String, Object> res = new HashMap<>();
        try {
            List<TrvDocumentDTO> docs = documentService.list(entityType, entityId);
            res.put("success", true);
            res.put("data", docs);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam String entityType,
                                       @RequestParam Long entityId,
                                       @RequestParam(defaultValue = "OTHER") String documentType,
                                       @RequestParam(required = false) String remarks,
                                       @RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            TrvDocumentDTO saved = documentService.upload(entityType, entityId, documentType, file, remarks);
            res.put("success", true);
            res.put("document", saved);
            res.put("message", "\"" + saved.getOriginalFileName() + "\" uploaded.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        TravelDocumentService.DownloadPayload payload = documentService.download(id);
        String encodedName = URLEncoder.encode(payload.originalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(payload.contentType() != null
                ? MediaType.parseMediaType(payload.contentType()) : MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
            .body(payload.resource());
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { documentService.delete(id); res.put("success", true); res.put("message", "Document deleted."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
