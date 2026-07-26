package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.dto.PassportOcrResult;
import com.asg.spindleserp.travel.service.PassportOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelOcrController — OCR endpoints for extracting passenger data from
 * uploaded passport / travel document images. Consumed by the air-tickets
 * and booking pages to auto-fill passenger fields.
 *
 * Endpoints:
 *   POST /travel/ocr/passport  (multipart: file)  →  PassportOcrResult JSON
 */
@Slf4j
@RestController
@RequestMapping("/travel/ocr")
@RequiredArgsConstructor
public class TravelOcrController {

    private final PassportOcrService ocrService;

    /**
     * Upload a passport image and receive extracted passenger data.
     *
     * @param file the passport image file (JPG, PNG, TIFF)
     * @return map with success flag and extracted data (or error message)
     */
    @PostMapping(value = "/passport", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> scanPassport(@RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        try {
            PassportOcrResult result = ocrService.extractPassportData(file);
            res.put("success", true);
            res.put("data", result);
            res.put("message", "Passport data extracted successfully.");
        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("Passport OCR failed: {}", e.getMessage(), e);
            res.put("success", false);
            res.put("message", "OCR processing error: " + e.getMessage());
        }
        return res;
    }
}
