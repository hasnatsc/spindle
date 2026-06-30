// Path: com/asg/spindleserp/ecommerce/productSupport/service/EcProductImageStorageService.java
package com.asg.spindleserp.ecommerce.productSupport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * EcProductImageStorageService — physical file I/O for product images.
 *
 * Deliberately separate from EcProductImageService (JPA/DB concern) so
 * the storage backend can be swapped (local disk → S3 / GCS / CDN) without
 * touching any DB-facing code. Today's implementation writes to the local
 * filesystem under app.upload-dir (see WebMvcConfig patch), organized as:
 *
 *   {uploadDir}/products/{productId}/{uuid}.{ext}
 *
 * Served back to the browser at:
 *
 *   /uploads/products/{productId}/{uuid}.{ext}
 *
 * Validates content-type and size before touching disk; never trusts the
 * client-supplied filename for path construction (prevents path traversal).
 */
@Slf4j
@Service
public class EcProductImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB, matches multipart config

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * Saves an uploaded image file to disk under products/{productId}/.
     * Returns the server-relative URL to store in EcProductImage.imageUrl.
     */
    public String store(Long productId, MultipartFile file) {
        validate(file);

        String ext = extensionOf(file.getOriginalFilename(), file.getContentType());
        String filename = UUID.randomUUID() + ext;

        try {
            Path productDir = Paths.get(uploadDir, "products", String.valueOf(productId));
            Files.createDirectories(productDir);

            Path target = productDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/products/" + productId + "/" + filename;
            log.info("Stored product image: productId={} url={} size={}B", productId, url, file.getSize());
            return url;
        } catch (IOException e) {
            log.error("Failed to store product image for productId={}", productId, e);
            throw new IllegalStateException("Failed to save uploaded image: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the physical file backing a stored image URL.
     * Safe no-op if the URL doesn't point under our managed uploads tree
     * (e.g. a manually-pasted external CDN URL) — only files we wrote
     * ourselves are ever deleted from disk.
     */
    public void delete(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith("/uploads/")) {
            return; // external URL — nothing on our disk to clean up
        }
        try {
            // Strip the leading "/uploads/" to get the path relative to uploadDir
            String relative = imageUrl.substring("/uploads/".length());
            Path target = Paths.get(uploadDir, relative).normalize();
            Path uploadRoot = Paths.get(uploadDir).normalize();

            // Guard against path traversal — the resolved path must stay inside uploadDir
            if (!target.startsWith(uploadRoot)) {
                log.warn("Refusing to delete path outside upload directory: {}", imageUrl);
                return;
            }
            Files.deleteIfExists(target);
            log.info("Deleted product image file: {}", imageUrl);
        } catch (IOException e) {
            // Deletion failures shouldn't block the DB-row delete the caller is performing —
            // log and move on; an orphaned file on disk is recoverable, a stuck UI is not.
            log.warn("Failed to delete image file for url={}: {}", imageUrl, e.getMessage());
        }
    }

    /** Bulk delete — used when an entire product (and all its images) is removed. */
    public void deleteAll(List<String> imageUrls) {
        if (imageUrls == null) return;
        imageUrls.forEach(this::delete);
    }

    // ── VALIDATION ───────────────────────────────────────────────────────────
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("No file was uploaded.");
        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("Image exceeds the 10MB size limit.");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP, or GIF images are allowed.");
    }

    private String extensionOf(String originalFilename, String contentType) {
        // Prefer a clean extension derived from content-type (never trust the
        // client-supplied filename directly — it could contain path separators).
        return switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default            -> ".jpg"; // covers image/jpeg and image/jpg
        };
    }
}
