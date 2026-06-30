// Path: com/asg/spindleserp/ecommerce/productSupport/controller/EcProductImageController.java
package com.asg.spindleserp.ecommerce.productSupport.controller;

import com.asg.spindleserp.ecommerce.productSupport.dto.EcProductImageDTO;
import com.asg.spindleserp.ecommerce.productSupport.service.EcProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EcProductImageController  /ecommerce/products/{productId}/images
 *
 * Dedicated single-image endpoints — separate from the bulk product
 * save path (EcProductCatalogController#save, which still uses
 * syncImages() for the "rebuild whole gallery" case from the form).
 *
 * JS fns: ecpImgUpload / ecpImgUpdate / ecpImgDelete / ecpImgList / ecpImgReplace
 *
 * Response envelope matches the rest of the app: { success, message, obj }
 */
@Slf4j
@RestController
@RequestMapping("/ecommerce/products/{productId}/images")
@RequiredArgsConstructor
public class EcProductImageController {

    private final EcProductImageService imageService;

    // ── LIST (gallery for view/edit) ─────────────────────────────────────────
    @GetMapping
    public List<EcProductImageDTO> list(@PathVariable Long productId) {
        return imageService.findByProduct(productId);
    }

    // ── SHOW single image ────────────────────────────────────────────────────
    @GetMapping("/{imageId}")
    public Map<String, Object> show(@PathVariable Long productId, @PathVariable Long imageId) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("success", true);
            res.put("obj", imageService.findById(productId, imageId));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── UPLOAD new image file ────────────────────────────────────────────────
    // multipart/form-data: file=<binary>, altText=<string>, isPrimary=<bool>
    @PostMapping(consumes = "multipart/form-data")
    public Map<String, Object> upload(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary) {

        Map<String, Object> res = new HashMap<>();
        try {
            EcProductImageDTO saved = imageService.upload(productId, file, altText, isPrimary);
            res.put("success", true);
            res.put("message", "Image uploaded successfully.");
            res.put("obj", saved);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── ADD by external URL (manual CDN-paste fallback, kept for flexibility) ─
    @PostMapping("/by-url")
    public Map<String, Object> addByUrl(@PathVariable Long productId, @RequestBody EcProductImageDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcProductImageDTO saved = imageService.addByUrl(productId, dto);
            res.put("success", true);
            res.put("message", "Image added successfully.");
            res.put("obj", saved);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── UPDATE metadata (alt text, order, primary) — no file change ─────────
    @PutMapping("/{imageId}")
    public Map<String, Object> update(@PathVariable Long productId, @PathVariable Long imageId,
                                       @RequestBody EcProductImageDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcProductImageDTO updated = imageService.update(productId, imageId, dto);
            res.put("success", true);
            res.put("message", "Image updated successfully.");
            res.put("obj", updated);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── REPLACE the file behind an existing image row ────────────────────────
    @PutMapping(value = "/{imageId}/file", consumes = "multipart/form-data")
    public Map<String, Object> replaceFile(@PathVariable Long productId, @PathVariable Long imageId,
                                            @RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            EcProductImageDTO updated = imageService.replaceFile(productId, imageId, file);
            res.put("success", true);
            res.put("message", "Image replaced successfully.");
            res.put("obj", updated);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── DELETE one image ──────────────────────────────────────────────────────
    @DeleteMapping("/{imageId}")
    public Map<String, Object> delete(@PathVariable Long productId, @PathVariable Long imageId) {
        Map<String, Object> res = new HashMap<>();
        try {
            imageService.delete(productId, imageId);
            res.put("success", true);
            res.put("message", "Image deleted successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
}
