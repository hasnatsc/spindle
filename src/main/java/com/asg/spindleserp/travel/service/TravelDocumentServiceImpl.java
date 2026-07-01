package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvDocumentDTO;
import com.asg.spindleserp.travel.entity.TrvDocument;
import com.asg.spindleserp.travel.repository.TrvDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TravelDocumentServiceImpl implements TravelDocumentService {

    private final TrvDocumentRepository documentRepo;

    /** Root directory for all Travel-module uploads. Configure in application.yml:
     *  spindle:
     *    upload:
     *      dir: /var/spindle/uploads       # or an absolute path on your server
     */
    @Value("${spindle.upload.dir:/var/spindle/uploads}")
    private String uploadRoot;

    private static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "jpg", "jpeg", "png", "webp", "doc", "docx");

    // =========================================================================
    // UPLOAD
    // =========================================================================

    @Override
    public TrvDocumentDTO upload(String entityType, Long entityId, String documentType,
                                  MultipartFile file, String remarks) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("No file was uploaded.");
        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("File exceeds the 15 MB limit.");

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase()))
            throw new IllegalArgumentException(
                "File type ." + extension + " is not allowed. Allowed: " + ALLOWED_EXTENSIONS);

        TrvDocument.EntityType type = TrvDocument.EntityType.valueOf(entityType);
        Long orgId = SecurityHelper.requireOrgId();

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path dir = Paths.get(uploadRoot, "travel", type.name(), String.valueOf(entityId));

        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName).normalize();
            if (!target.startsWith(dir))
                throw new IllegalArgumentException("Invalid file name."); // path traversal guard
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file: " + e.getMessage(), e);
        }

        TrvDocument doc = TrvDocument.builder()
            .entityType(type)
            .entityId(entityId)
            .documentType(documentType != null
                ? TrvDocument.DocumentType.valueOf(documentType) : TrvDocument.DocumentType.OTHER)
            .originalFileName(originalName)
            .storedFileName(storedName)
            .filePath(dir.resolve(storedName).toString())
            .contentType(file.getContentType())
            .fileSizeBytes(file.getSize())
            .remarks(remarks)
            .build();

        String user = SecurityHelper.currentUsername().orElse("system");
        doc.setCreatedBy(user);
        doc.setUpdatedBy(user);

        return toDTO(documentRepo.save(doc));
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TrvDocumentDTO> list(String entityType, Long entityId) {
        TrvDocument.EntityType type = TrvDocument.EntityType.valueOf(entityType);
        return documentRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(type, entityId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // =========================================================================
    // DOWNLOAD
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DownloadPayload download(Long documentId) {
        TrvDocument doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document #" + documentId + " not found."));

        try {
            Path path = Paths.get(doc.getFilePath()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new IllegalStateException(
                    "Stored file is missing on disk for document #" + documentId + " — it may have been moved or deleted outside the app.");
            return new DownloadPayload(resource, doc.getOriginalFileName(), doc.getContentType());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not resolve stored file path: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    public void delete(Long documentId) {
        TrvDocument doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document #" + documentId + " not found."));

        try {
            Path path = Paths.get(doc.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Don't block the DB delete on a filesystem hiccup — log and continue,
            // otherwise a locked/already-missing file would make the row undeletable.
            log.warn("Could not delete file on disk for document #{}: {}", documentId, e.getMessage());
        }

        documentRepo.deleteById(documentId);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private TrvDocumentDTO toDTO(TrvDocument e) {
        return TrvDocumentDTO.builder()
            .id(e.getId())
            .entityType(e.getEntityType().name())
            .entityId(e.getEntityId())
            .documentType(e.getDocumentType().name())
            .originalFileName(e.getOriginalFileName())
            .contentType(e.getContentType())
            .fileSizeBytes(e.getFileSizeBytes())
            .remarks(e.getRemarks())
            .uploadedBy(e.getCreatedBy())
            .uploadedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .downloadUrl("/travel/documents/download/" + e.getId())
            .build();
    }

    /** Strips any directory components and control characters — defends against path traversal via filename. */
    private String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "file";
        String name = Paths.get(rawName).getFileName().toString();
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : "";
    }
}
