package com.asg.spindleserp.travel.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * PassportOcrEngine — ★ INTEGRATION SEAM ★
 *
 * Optional. Spindle ships with NO implementation of this interface, and that
 * is deliberate:
 *
 *   • Tesseract via tess4j needs a native libtesseract + traineddata on every
 *     box the WAR is deployed to. That is a real operational burden for a
 *     feature used a few times a day.
 *   • Cloud OCR (Google Vision, Azure Read, AWS Textract) means passport
 *     images — the most sensitive PII the travel desk touches — leave ASG's
 *     network. That is a decision for the business, not a default.
 *
 * So the default path runs OCR in the operator's browser (Tesseract.js, see
 * static/js/travel-passport-scan.js) and posts only the extracted MRZ *text*
 * to the server, where MrzParser does the real work. The image never has to
 * leave the machine unless the operator explicitly attaches it.
 *
 * If you later want server-side OCR, register ONE @Service implementing this
 * interface. TravelPassengerServiceImpl picks it up automatically through
 * constructor injection of Optional&lt;PassportOcrEngine&gt; — no other file
 * changes, no configuration flag.
 *
 * Example skeleton (add net.sourceforge.tess4j:tess4j to pom.xml first):
 *
 * <pre>
 * &#64;Service
 * &#64;RequiredArgsConstructor
 * public class TesseractPassportOcrEngine implements PassportOcrEngine {
 *     &#64;Value("${spindle.ocr.tessdata:/usr/share/tesseract-ocr/4.00/tessdata}")
 *     private String tessdata;
 *
 *     &#64;Override
 *     public String extractText(MultipartFile file) throws Exception {
 *         Tesseract t = new Tesseract();
 *         t.setDatapath(tessdata);
 *         t.setLanguage("eng");
 *         t.setPageSegMode(6);
 *         t.setVariable("tessedit_char_whitelist", MRZ_WHITELIST);
 *         return t.doOCR(ImageIO.read(file.getInputStream()));
 *     }
 * }
 * </pre>
 */
public interface PassportOcrEngine {

    /**
     * The only characters that can legally appear in an MRZ. Restricting the
     * OCR alphabet to these is the single biggest accuracy win available —
     * it removes every lowercase and punctuation mis-read in one move.
     */
    String MRZ_WHITELIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<";

    /**
     * Returns the raw recognised text of a passport image. Line breaks matter:
     * MrzParser locates the MRZ by scanning line by line, so preserve them.
     *
     * @throws Exception any failure — the caller converts it into a friendly
     *                   message and falls back to manual MRZ entry.
     */
    String extractText(MultipartFile file) throws Exception;

    /** Short label shown to the operator, e.g. "Tesseract 5 (server)". */
    default String engineName() {
        return getClass().getSimpleName();
    }
}
