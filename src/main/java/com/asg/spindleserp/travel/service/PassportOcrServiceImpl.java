package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.PassportOcrResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PassportOcrServiceImpl — extracts passenger data from passport images
 * using Tesseract OCR with MRZ (Machine Readable Zone) and visual zone parsing.
 *
 * The OCR engine is configured via {@code app.ocr.tesseract-data-path} (defaults
 * to the system-installed Tesseract data directory). Set this property if
 * Tesseract is installed in a non-standard location, or to use a bundled
 * tessdata directory on the classpath.
 */
@Slf4j
@Service
public class PassportOcrServiceImpl implements PassportOcrService {

    private static final Pattern MRZ_LINE1 = Pattern.compile(
            "^([PVD])([A-Z<]{3})([A-Z<]+)<<(.+)$");
    private static final Pattern MRZ_LINE2 = Pattern.compile(
            "^([A-Z0-9<]{9})([0-9])" // passport number + check digit
            + "([A-Z<]{3})"           // nationality
            + "(\\d{6})([0-9])"       // DOB + check digit
            + "([MFX<])"              // gender
            + "(\\d{6})([0-9])"       // expiry + check digit
            + "(.+)$");               // personal number + final check

    private static final Pattern PREFIX_DOB = Pattern.compile(
            "(?i)(?:date\\s*of\\s*birth|dob|birth\\s*date|born)\\s*[:.]?\\s*(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})");
    private static final Pattern PREFIX_EXPIRY = Pattern.compile(
            "(?i)(?:date\\s*of\\s*expiry|expiry\\s*date|expires|expiration|date\\s*d\\s*expiration|valid\\s*until)\\s*[:.]?\\s*(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})");
    private static final Pattern PREFIX_PASSPORT_NO = Pattern.compile(
            "(?i)(?:passport\\s*no|passport\\s*number|pass\\s*no\\.?|document\\s*no)\\s*[:.]?\\s*([A-Z0-9]{4,20})");
    private static final Pattern PREFIX_NATIONALITY = Pattern.compile(
            "(?i)(?:nationality|citizenship|nationalit[ée])\\s*[:.]?\\s*([A-Za-z]{2,})");
    private static final Pattern PREFIX_GIVEN_NAMES = Pattern.compile(
            "(?i)(?:given\\s*names|first\\s*name|given\\s*name)\\s*[:.]?\\s*([A-Za-z\\s-]+)");
    private static final Pattern PREFIX_SURNAME = Pattern.compile(
            "(?i)(?:surname|last\\s*name|family\\s*name)\\s*[:.]?\\s*([A-Za-z\\s-]+)");

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd.MM.yy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yy/MM/dd"),
            DateTimeFormatter.ofPattern("yyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    @Value("${app.ocr.tesseract-data-path:#{null}}")
    private String tessDataPath;

    private Tesseract tesseract;

    private String resolvedDatapath;

    @PostConstruct
    public void init() {
        tesseract = new Tesseract();
        if (tessDataPath != null && !tessDataPath.isBlank()) {
            resolvedDatapath = tessDataPath;
        } else {
            // Common default paths by OS
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                resolvedDatapath = "C:/Program Files/Tesseract-OCR/tessdata";
            } else {
                resolvedDatapath = "/usr/share/tesseract-ocr/4.00/tessdata";
            }
        }
        tesseract.setDatapath(resolvedDatapath);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(6); // Assume uniform block of text (good for passports)
        log.info("PassportOCR initialised with tessdata: {}", resolvedDatapath);
    }

    @Override
    public PassportOcrResult extractPassportData(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Passport image file is required.");
        }
        validateImageContentType(file.getOriginalFilename());

        Path tempFile = null;
        try {
            // Write to a temp file (Tess4J works best with files, not BufferedImages for MRZ)
            tempFile = Files.createTempFile("passport_ocr_", getExtension(file.getOriginalFilename()));
            file.transferTo(tempFile.toFile());

            // Attempt full OCR
            String ocrText = tesseract.doOCR(tempFile.toFile());
            log.debug("Raw OCR text (first 500 chars): {}", ocrText.length() > 500 ? ocrText.substring(0, 500) : ocrText);

            // Attempt MRZ-specific extraction with a focused OCR pass (higher DPI / smaller region)
            String mrzText = extractMrz(tempFile);
            log.debug("MRZ text ({} chars): {}", mrzText != null ? mrzText.length() : 0, mrzText);

            PassportOcrResult result = parsePassportData(ocrText, mrzText);
            result.setRawOcrText(ocrText);
            result.setMrzText(mrzText);

            log.info("Passport OCR complete{}: {} {} / {} / {}",
                    result.isFromMrz() ? " (from MRZ)" : "",
                    result.getFirstName(), result.getLastName(),
                    result.getPassportNumber(), result.getNationality());
            return result;

        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage(), e);
            throw new RuntimeException("Passport OCR processing failed: " + e.getMessage()
                    + ". Ensure Tesseract is installed. On Windows: https://github.com/UB-Mannheim/tesseract/wiki", e);
        } catch (IOException e) {
            log.error("Failed to process passport image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process passport image: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Extract the MRZ region by doing a second OCR pass with configuration
     * tuned for the small, fixed-width font used in the machine-readable zone.
     */
    private String extractMrz(Path imagePath) {
        try {
            Tesseract mrzTess = new Tesseract();
            mrzTess.setDatapath(resolvedDatapath);
            mrzTess.setLanguage("eng");
            // PSM 6 = uniform block; PSM 3 = automatic; PSM 7 = single text line
            mrzTess.setPageSegMode(7); // Single text line — MRZ lines are isolated
            mrzTess.setVariable("tessedit_char_whitelist",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<");
            String text = mrzTess.doOCR(imagePath.toFile());
            // Filter to only lines that look like MRZ (35-44 chars, mostly uppercase + <)
            StringBuilder sb = new StringBuilder();
            for (String line : text.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.length() >= 30 && trimmed.matches("[A-Z0-9<]+")) {
                    sb.append(trimmed).append("\n");
                }
            }
            String result = sb.toString().trim();
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.debug("MRZ extraction attempt failed (non-fatal): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse passenger data from the combined OCR and MRZ text.
     * MRZ text takes priority where available.
     */
    private PassportOcrResult parsePassportData(String ocrText, String mrzText) {
        PassportOcrResult result = new PassportOcrResult();

        // ── Try MRZ first (more reliable) ───────────────────────────────────
        if (mrzText != null && !mrzText.isBlank()) {
            String[] lines = mrzText.split("\\n");
            if (lines.length >= 1) {
                Matcher m1 = MRZ_LINE1.matcher(lines[0].trim());
                if (m1.matches()) {
                    String surnames = m1.group(3).replace('<', ' ').trim().replaceAll("\\s+", " ");
                    String givenNames = m1.group(4).replace('<', ' ').trim().replaceAll("\\s+", " ");
                    result.setLastName(surnames.isEmpty() ? null : surnames);
                    result.setFirstName(givenNames.isEmpty() ? null : givenNames);
                    result.setFromMrz(true);
                }
            }
            if (lines.length >= 2) {
                Matcher m2 = MRZ_LINE2.matcher(lines[1].trim());
                if (m2.matches()) {
                    String passNo = m2.group(1).replace('<', ' ').trim();
                    result.setPassportNumber(passNo.isEmpty() ? null : passNo);

                    String nationality = m2.group(3).replace('<', ' ').trim();
                    result.setNationality(nationality.isEmpty() ? null : nationality);

                    String dobStr = m2.group(4); // YYMMDD
                    if (!dobStr.isEmpty() && !dobStr.matches("<+")) {
                        result.setDateOfBirth(parseMrzDate(dobStr));
                    }

                    String expiryStr = m2.group(7); // YYMMDD
                    if (!expiryStr.isEmpty() && !expiryStr.matches("<+")) {
                        result.setPassportExpiry(parseMrzDate(expiryStr));
                    }
                    result.setFromMrz(true);
                }
            }
        }

        // ── Visual zone fallback for any missing fields ─────────────────────

        if (result.getFirstName() == null || result.getFirstName().isBlank()) {
            Matcher m = PREFIX_GIVEN_NAMES.matcher(ocrText);
            if (m.find()) {
                result.setFirstName(m.group(1).trim());
            }
        }
        if (result.getLastName() == null || result.getLastName().isBlank()) {
            Matcher m = PREFIX_SURNAME.matcher(ocrText);
            if (m.find()) {
                result.setLastName(m.group(1).trim());
            }
        }
        if (result.getPassportNumber() == null || result.getPassportNumber().isBlank()) {
            Matcher m = PREFIX_PASSPORT_NO.matcher(ocrText);
            if (m.find()) {
                result.setPassportNumber(m.group(1).trim());
            }
        }
        if (result.getNationality() == null || result.getNationality().isBlank()) {
            Matcher m = PREFIX_NATIONALITY.matcher(ocrText);
            if (m.find()) {
                result.setNationality(m.group(1).trim());
            }
        }
        if (result.getDateOfBirth() == null) {
            Matcher m = PREFIX_DOB.matcher(ocrText);
            if (m.find()) {
                result.setDateOfBirth(parseDateFlexible(m.group(1).trim()));
            }
        }
        if (result.getPassportExpiry() == null) {
            Matcher m = PREFIX_EXPIRY.matcher(ocrText);
            if (m.find()) {
                result.setPassportExpiry(parseDateFlexible(m.group(1).trim()));
            }
        }

        // ── Attempt line-by-line heuristics for common passport layouts ──────
        if (result.getPassportNumber() == null || result.getNationality() == null) {
            applyLineHeuristics(ocrText, result);
        }

        return result;
    }

    /**
     * Heuristic scanning for common passport visual-zone layouts.
     * Many passports list fields as "P<code>" patterns or "PASSORT NO / NUMBER" legends.
     */
    private void applyLineHeuristics(String text, PassportOcrResult result) {
        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) continue;

            // Passport number often sits on a line that starts with a letter followed by digits
            if (result.getPassportNumber() == null) {
                Matcher m = Pattern.compile("(?i)(?:passport|pass|doc\\.?)\\s*[#.:]?\\s*([A-Z][0-9A-Z]{4,})").matcher(line);
                if (m.find()) {
                    result.setPassportNumber(m.group(1).trim());
                }
            }

            // Nationality: often a single 3-letter code on its own line near "NATIONALITY"
            if (result.getNationality() == null) {
                Matcher m = Pattern.compile("(?i)nationality\\s*[:.]?\\s*([A-Z]{3})").matcher(line);
                if (m.find()) {
                    result.setNationality(m.group(1).trim());
                }
            }
        }

        // If nationality is still null, look for standalone 3-letter codes near the passport number
        if (result.getNationality() == null) {
            for (String line : lines) {
                line = line.trim();
                Matcher m = Pattern.compile("\\b([A-Z]{3})\\b").matcher(line);
                while (m.find()) {
                    String code = m.group(1);
                    // Common ICAO nationality codes
                    if (isIcaoNationality(code)) {
                        result.setNationality(code);
                        break;
                    }
                }
                if (result.getNationality() != null) break;
            }
        }
    }

    /** Parse MRZ date format YYMMDD → LocalDate (with century heuristic). */
    private LocalDate parseMrzDate(String yymmdd) {
        if (yymmdd == null || yymmdd.length() < 6) return null;
        try {
            int year = Integer.parseInt(yymmdd.substring(0, 2));
            int month = Integer.parseInt(yymmdd.substring(2, 4));
            int day = Integer.parseInt(yymmdd.substring(4, 6));
            int fullYear = 2000 + year; // passports valid max 10y, so 2000+ is safe
            return LocalDate.of(fullYear, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /** Parse date from various common formats. */
    private LocalDate parseDateFlexible(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String cleaned = dateStr.replaceAll("\\s+", "").replaceAll("[-/.]", "/");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        // Try replacing / with - for single-digit days/months
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                String alt = cleaned.replace("/", "-");
                return LocalDate.parse(alt, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /** Rough check for common ICAO 3-letter nationality codes. */
    private boolean isIcaoNationality(String code) {
        if (code == null || code.length() != 3) return false;
        String upper = code.toUpperCase();
        return switch (upper) {
            case "BGD", "USA", "GBR", "CAN", "AUS", "DEU", "FRA", "ITA", "ESP",
                 "NLD", "BEL", "CHE", "SWE", "NOR", "DNK", "FIN", "JPN", "KOR",
                 "CHN", "IND", "PAK", "NPL", "LKA", "MDV", "THA", "MYS", "SGP",
                 "IDN", "PHL", "VNM", "ARE", "SAU", "QAT", "KWT", "OMN", "BHR",
                 "ISR", "TUR", "RUS", "UKR", "ZAF", "EGY", "MAR", "TUN", "DZA",
                 "NGA", "KEN", "ETH", "BRA", "ARG", "MEX", "COL", "CHL", "PER" -> true;
            default -> false;
        };
    }

    private void validateImageContentType(String filename) {
        if (filename == null) throw new IllegalArgumentException("File name is required.");
        String lower = filename.toLowerCase();
        if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".tif")
                || lower.endsWith(".tiff") || lower.endsWith(".webp")
                || lower.endsWith(".pdf"))) {
            throw new IllegalArgumentException("Unsupported file format: " + filename
                    + ". Supported: JPG, PNG, TIFF, WEBP, PDF");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : ".jpg";
    }
}
