package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.dto.CommonDocumentDTO;
import com.asg.spindleserp.setup.entity.DocumentFile;
import com.asg.spindleserp.setup.repository.DocumentFileRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CommonDocumentServiceImpl — shared file attachment service for ALL modules.
 *
 * Stores files on the local filesystem under spindle.upload.dir/common/
 * organised by documentType/referenceId.
 *
 * Thread-safe: each upload generates a new UUID stored name so concurrent
 * uploads for the same reference never collide.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommonDocumentServiceImpl implements CommonDocumentService {

    private final DocumentFileRepository documentFileRepo;

    @Value("${spindle.upload.dir:/var/spindle/uploads}")
    private String uploadRoot;

    private static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "webp", "gif", "doc", "docx", "xls", "xlsx", "txt", "csv");

    // =========================================================================
    // UPLOAD
    // =========================================================================

    @Override
    public CommonDocumentDTO upload(String documentType, Long referenceId,
                                    String documentCategory, MultipartFile file, String remarks) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("No file was uploaded.");
        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("File exceeds the 15 MB limit.");

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase()))
            throw new IllegalArgumentException(
                    "File type ." + extension + " is not allowed. Allowed: " + ALLOWED_EXTENSIONS);

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path dir = Paths.get(uploadRoot, "common", documentType, String.valueOf(referenceId));
        Path target;

        try {
            Files.createDirectories(dir);
            target = dir.resolve(storedName).normalize();
            if (!target.startsWith(dir))
                throw new IllegalArgumentException("Invalid file name."); // path traversal guard
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file: " + e.getMessage(), e);
        }

        String user = SecurityHelper.currentUsername().orElse("system");

        DocumentFile doc = DocumentFile.builder()
                .documentType(documentType)
                .referenceId(referenceId)
                .documentCategory(documentCategory)
                .fileName(storedName)
                .originalFileName(originalName)
                .fileType(file.getContentType())
                .filePath(target.toString())
                .fileSize(file.getSize())
                .remarks(remarks)
                .uploadedAt(LocalDateTime.now())
                .uploadedBy(user)
                .build();

        return toDTO(documentFileRepo.save(doc));
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CommonDocumentDTO> list(String documentType, Long referenceId) {
        return documentFileRepo.findByDocumentTypeAndReferenceId(documentType, referenceId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // =========================================================================
    // DOWNLOAD
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DownloadPayload download(Long documentId) {
        DocumentFile doc = documentFileRepo.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document #" + documentId + " not found."));

        try {
            Path path = Paths.get(doc.getFilePath()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new IllegalStateException(
                        "Stored file is missing on disk for document #" + documentId);
            return new DownloadPayload(resource, doc.getOriginalFileName(), doc.getFileType());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not resolve stored file path: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    public void delete(Long documentId) {
        DocumentFile doc = documentFileRepo.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document #" + documentId + " not found."));

        try {
            Path path = Paths.get(doc.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete file on disk for doc #{}: {}", documentId, e.getMessage());
        }

        documentFileRepo.deleteById(documentId);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private CommonDocumentDTO toDTO(DocumentFile e) {
        return CommonDocumentDTO.builder()
                .id(e.getId())
                .documentType(e.getDocumentType())
                .referenceId(e.getReferenceId())
                .documentCategory(e.getDocumentCategory())
                .originalFileName(e.getOriginalFileName())
                .fileType(e.getFileType())
                .fileSize(e.getFileSize())
                .remarks(e.getRemarks())
                .uploadedBy(e.getUploadedBy())
                .uploadedAt(e.getUploadedAt() != null ? e.getUploadedAt().toString() : null)
                .downloadUrl("/api/common/documents/download/" + e.getId())
                .build();
    }

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
