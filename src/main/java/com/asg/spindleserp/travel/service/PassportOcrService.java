package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.PassportOcrResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * PassportOcrService — OCR extraction of passenger data from passport images.
 *
 * Supports both the visual zone (name, DOB, nationality) and the MRZ
 * (Machine Readable Zone) at the bottom of ICAO‑compliant passports.
 * MRZ parsing is preferred where available because it is far more reliable
 * than free‑form OCR.
 */
public interface PassportOcrService {

    /**
     * Extract passenger information from a passport image.
     *
     * @param file the uploaded passport image (JPEG, PNG, or TIFF preferred)
     * @return parsed passport fields; never null, but fields may be null
     *         if they could not be extracted
     * @throws IllegalArgumentException if the file is empty or not an image
     * @throws RuntimeException         if OCR processing fails
     */
    PassportOcrResult extractPassportData(MultipartFile file);
}
