package com.asg.spindleserp.common.util;

import com.asg.spindleserp.travel.dto.PassportScanDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * MrzParser — pure-Java ICAO 9303 Machine Readable Zone parser.
 *
 * Supports all three ICAO document formats:
 *   TD3 — passport booklet, 2 lines x 44 chars   (the common case)
 *   TD2 — passport card / older visa, 2 x 36
 *   TD1 — ID card / NID / residence permit, 3 x 30
 *
 * Design notes
 * ------------
 * 1. NO external dependency, NO native library, NO OCR here. This class turns
 *    *text* into structured data. Getting the text off a photo is the OCR
 *    layer's job (browser-side Tesseract.js by default — see
 *    static/js/travel-passport-scan.js — or a server-side PassportOcrEngine
 *    bean if one is registered).
 *
 * 2. OCR output is dirty, so parsing is position-aware: every MRZ position has
 *    a known type (numeric / alpha / filler). Digits found in an alpha field
 *    are folded to letters and letters found in a numeric field are folded to
 *    digits BEFORE extraction. This alone fixes the overwhelming majority of
 *    real-world scan errors (0/O, 1/I, 5/S, 8/B, 2/Z, 6/G).
 *
 * 3. Every ICAO check digit is recomputed. A mismatch never aborts the parse —
 *    the value is still returned, but a human-readable warning is attached so
 *    the operator knows which specific field to eyeball before saving.
 *
 * Verified against a live Bangladeshi e-passport (BGD / TD3): all five check
 * digits — document number, DOB, expiry, personal number, composite — match.
 */
public final class MrzParser {

    private MrzParser() { }

    private static final int[] WEIGHTS = {7, 3, 1};

    /**
     * Letters OCR commonly mistakes for digits, applied in NUMERIC-only MRZ
     * positions.
     *
     * Written as an explicit map rather than two parallel strings on purpose:
     * parallel strings silently drift out of alignment when one side is edited,
     * and a one-character shift here corrupts dates and passport numbers while
     * still looking plausible on screen.
     */
    private static final Map<Character, Character> TO_DIGIT = Map.ofEntries(
            Map.entry('O', '0'), Map.entry('Q', '0'), Map.entry('D', '0'), Map.entry('U', '0'),
            Map.entry('I', '1'), Map.entry('L', '1'),
            Map.entry('Z', '2'),
            Map.entry('A', '4'),
            Map.entry('S', '5'),
            Map.entry('G', '6'), Map.entry('C', '6'),
            Map.entry('T', '7'),
            Map.entry('B', '8'));

    /** Digits OCR commonly mistakes for letters, applied in ALPHA-only positions. */
    private static final Map<Character, Character> TO_LETTER = Map.ofEntries(
            Map.entry('0', 'O'),
            Map.entry('1', 'I'),
            Map.entry('2', 'Z'),
            Map.entry('4', 'A'),
            Map.entry('5', 'S'),
            Map.entry('6', 'G'),
            Map.entry('7', 'T'),
            Map.entry('8', 'B'),
            Map.entry('9', 'Q'));
    // 3 is deliberately absent — it has no single unambiguous letter twin.

    // =========================================================================
    // PUBLIC ENTRY POINT
    // =========================================================================

    /**
     * Parses raw OCR text (or hand-typed MRZ lines) into a passport DTO.
     * Never throws for bad input — returns a DTO with success=false and a
     * message the UI can show verbatim.
     */
    public static PassportScanDTO parse(String rawText) {
        PassportScanDTO out = new PassportScanDTO();
        // Set the optimistic defaults by hand. @Builder.Default moves a field
        // initializer into the builder, so a plain `new PassportScanDTO()`
        // would leave checkDigitsValid false and warnings null — and every
        // scan would then report a check-digit failure it never had.
        out.setWarnings(new ArrayList<>());
        out.setCheckDigitsValid(true);
        out.setSuggestedPassengerType("ADULT");

        if (rawText == null || rawText.isBlank()) {
            out.setSuccess(false);
            out.setMessage("No MRZ text supplied. Scan the passport or type the two bottom lines manually.");
            return out;
        }

        List<String> lines = candidateLines(rawText);
        if (lines.isEmpty()) {
            out.setSuccess(false);
            out.setMessage("No machine-readable lines found. Make sure the bottom two lines of the "
                    + "passport data page are fully inside the frame, flat and well lit.");
            return out;
        }

        try {
            // TD1 is three 30-char lines; check it first so its shorter lines
            // are not mistaken for a truncated TD2.
            if (lines.size() >= 3 && near(lines.get(0).length(), 30)
                    && near(lines.get(1).length(), 30) && near(lines.get(2).length(), 30)) {
                return parseTd1(pad(lines.get(0), 30), pad(lines.get(1), 30), pad(lines.get(2), 30), out);
            }
            if (lines.size() >= 2 && near(lines.get(0).length(), 44) && near(lines.get(1).length(), 44)) {
                return parseTd3(pad(lines.get(0), 44), pad(lines.get(1), 44), out);
            }
            if (lines.size() >= 2 && near(lines.get(0).length(), 36) && near(lines.get(1).length(), 36)) {
                return parseTd2(pad(lines.get(0), 36), pad(lines.get(1), 36), out);
            }

            // Length is off but two lines are present — assume TD3 (by far the
            // most common) and let the check digits report what went wrong.
            if (lines.size() >= 2) {
                out.getWarnings().add("MRZ line length was unexpected (" + lines.get(0).length()
                        + "/" + lines.get(1).length() + " instead of 44/44) — read as a passport anyway. "
                        + "Please verify every field.");
                return parseTd3(pad(lines.get(0), 44), pad(lines.get(1), 44), out);
            }

            out.setSuccess(false);
            out.setMessage("Only one machine-readable line was detected. A passport MRZ has two lines — "
                    + "re-scan including the very bottom edge of the page.");
            return out;

        } catch (Exception ex) {
            out.setSuccess(false);
            out.setMessage("Could not read the MRZ: " + ex.getMessage()
                    + ". You can type the two bottom lines manually instead.");
            return out;
        }
    }

    // =========================================================================
    // TD3 — PASSPORT BOOKLET (2 x 44)
    // =========================================================================

    private static PassportScanDTO parseTd3(String l1, String l2, PassportScanDTO out) {
        out.setFormat("TD3");
        out.setMrzLine1(l1);
        out.setMrzLine2(l2);

        // ── Line 1 ────────────────────────────────────────────────────────────
        out.setDocumentCode(alpha(l1.substring(0, 2)).replace("<", "").trim());
        out.setIssuingCountry(alpha(l1.substring(2, 5)).replace("<", "").trim());
        applyNames(out, l1.substring(5, 44));

        // ── Line 2 ────────────────────────────────────────────────────────────
        String docNo      = l2.substring(0, 9);       // alphanumeric — no folding
        char   docNoCd    = l2.charAt(9);
        String nationality = alpha(l2.substring(10, 13));
        String dob        = numeric(l2.substring(13, 19));
        char   dobCd      = l2.charAt(19);
        char   sex        = l2.charAt(20);
        String expiry     = numeric(l2.substring(21, 27));
        char   expCd      = l2.charAt(27);
        String personalNo = l2.substring(28, 42);
        char   persCd     = l2.charAt(42);
        char   compositeCd = l2.charAt(43);

        out.setPassportNumber(strip(docNo));
        out.setNationality(strip(nationality));
        out.setPersonalNumber(strip(personalNo));
        out.setSex(sex == 'F' ? "F" : sex == 'M' ? "M" : "X");

        out.setDateOfBirth(toDate(dob, false, out, "date of birth"));
        out.setPassportExpiry(toDate(expiry, true, out, "passport expiry"));

        verify(out, docNo, docNoCd, "document number");
        verify(out, dob, dobCd, "date of birth");
        verify(out, expiry, expCd, "passport expiry");
        if (!isFillerOnly(personalNo)) verify(out, personalNo, persCd, "personal number");

        String composite = docNo + docNoCd + dob + dobCd + expiry + expCd + personalNo + persCd;
        verify(out, composite, compositeCd, "overall (composite)");

        return finish(out);
    }

    // =========================================================================
    // TD2 — 2 x 36
    // =========================================================================

    private static PassportScanDTO parseTd2(String l1, String l2, PassportScanDTO out) {
        out.setFormat("TD2");
        out.setMrzLine1(l1);
        out.setMrzLine2(l2);

        out.setDocumentCode(alpha(l1.substring(0, 2)).replace("<", "").trim());
        out.setIssuingCountry(alpha(l1.substring(2, 5)).replace("<", "").trim());
        applyNames(out, l1.substring(5, 36));

        String docNo   = l2.substring(0, 9);
        char   docNoCd = l2.charAt(9);
        String nat     = alpha(l2.substring(10, 13));
        String dob     = numeric(l2.substring(13, 19));
        char   dobCd   = l2.charAt(19);
        char   sex     = l2.charAt(20);
        String expiry  = numeric(l2.substring(21, 27));
        char   expCd   = l2.charAt(27);
        String optional = l2.substring(28, 35);
        char   compositeCd = l2.charAt(35);

        out.setPassportNumber(strip(docNo));
        out.setNationality(strip(nat));
        out.setPersonalNumber(strip(optional));
        out.setSex(sex == 'F' ? "F" : sex == 'M' ? "M" : "X");
        out.setDateOfBirth(toDate(dob, false, out, "date of birth"));
        out.setPassportExpiry(toDate(expiry, true, out, "passport expiry"));

        verify(out, docNo, docNoCd, "document number");
        verify(out, dob, dobCd, "date of birth");
        verify(out, expiry, expCd, "passport expiry");
        verify(out, docNo + docNoCd + dob + dobCd + expiry + expCd + optional, compositeCd, "overall (composite)");

        return finish(out);
    }

    // =========================================================================
    // TD1 — ID CARD / NID (3 x 30)
    // =========================================================================

    private static PassportScanDTO parseTd1(String l1, String l2, String l3, PassportScanDTO out) {
        out.setFormat("TD1");
        out.setMrzLine1(l1);
        out.setMrzLine2(l2);
        out.setMrzLine3(l3);

        out.setDocumentCode(alpha(l1.substring(0, 2)).replace("<", "").trim());
        out.setIssuingCountry(alpha(l1.substring(2, 5)).replace("<", "").trim());
        String docNo    = l1.substring(5, 14);
        char   docNoCd  = l1.charAt(14);
        String optional1 = l1.substring(15, 30);

        String dob     = numeric(l2.substring(0, 6));
        char   dobCd   = l2.charAt(6);
        char   sex     = l2.charAt(7);
        String expiry  = numeric(l2.substring(8, 14));
        char   expCd   = l2.charAt(14);
        String nat     = alpha(l2.substring(15, 18));
        String optional2 = l2.substring(18, 29);
        char   compositeCd = l2.charAt(29);

        applyNames(out, l3);

        out.setPassportNumber(strip(docNo));
        out.setNationality(strip(nat));
        out.setPersonalNumber(strip(optional1.isBlank() ? optional2 : optional1));
        out.setSex(sex == 'F' ? "F" : sex == 'M' ? "M" : "X");
        out.setDateOfBirth(toDate(dob, false, out, "date of birth"));
        out.setPassportExpiry(toDate(expiry, true, out, "expiry"));

        verify(out, docNo, docNoCd, "document number");
        verify(out, dob, dobCd, "date of birth");
        verify(out, expiry, expCd, "expiry");
        verify(out, docNo + docNoCd + optional1 + dob + dobCd + expiry + expCd + optional2,
                compositeCd, "overall (composite)");

        return finish(out);
    }

    // =========================================================================
    // SHARED FIELD LOGIC
    // =========================================================================

    /** Splits the ICAO name field: SURNAME&lt;&lt;GIVEN&lt;NAMES. */
    private static void applyNames(PassportScanDTO out, String nameField) {
        String field = alpha(nameField);
        int sep = field.indexOf("<<");
        String surname, given;
        if (sep >= 0) {
            surname = field.substring(0, sep);
            given   = field.substring(sep + 2);
        } else {
            surname = field;
            given   = "";
        }
        out.setSurname(tidyName(surname));
        out.setGivenNames(tidyName(given));

        if (out.getSurname().isEmpty() && out.getGivenNames().isEmpty())
            out.getWarnings().add("The name field came back empty — re-scan or type the name in manually.");
    }

    /** Turns FILLER-separated MRZ names into "Title Case Words". */
    private static String tidyName(String raw) {
        String spaced = raw.replace('<', ' ').trim().replaceAll("\\s+", " ");
        if (spaced.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String word : spaced.split(" ")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            // Single letters stay upper-case (initials such as "H M").
            sb.append(word.length() == 1
                    ? word
                    : word.charAt(0) + word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * YYMMDD to LocalDate.
     * expiryStyle=false → birth dates: a two-digit year above the current one
     *                     must belong to the previous century.
     * expiryStyle=true  → validity dates: never far in the past, so roll a
     *                     stale-looking year forward a century.
     */
    private static LocalDate toDate(String yymmdd, boolean expiryStyle,
                                    PassportScanDTO out, String label) {
        if (yymmdd == null || yymmdd.length() != 6 || !yymmdd.chars().allMatch(Character::isDigit)) {
            out.getWarnings().add("The " + label + " digits could not be read — please enter it manually.");
            return null;
        }
        int yy = Integer.parseInt(yymmdd.substring(0, 2));
        int mm = Integer.parseInt(yymmdd.substring(2, 4));
        int dd = Integer.parseInt(yymmdd.substring(4, 6));
        if (mm < 1 || mm > 12 || dd < 1 || dd > 31) {
            out.getWarnings().add("The " + label + " (" + yymmdd + ") is not a valid date — please enter it manually.");
            return null;
        }
        int currentYear = LocalDate.now().getYear();
        int century = expiryStyle
                ? (2000 + yy < currentYear - 10 ? 2100 : 2000)
                : (2000 + yy > currentYear ? 1900 : 2000);
        try {
            return LocalDate.of(century + yy, mm, dd);
        } catch (Exception e) {
            out.getWarnings().add("The " + label + " (" + yymmdd + ") is not a real calendar date.");
            return null;
        }
    }

    /** Computes and compares one ICAO 7-3-1 check digit. */
    private static void verify(PassportScanDTO out, String data, char expected, String label) {
        if (expected == '<') return;                 // field genuinely unused
        int calculated = checkDigit(data);
        if (!Character.isDigit(expected) || calculated != Character.getNumericValue(expected)) {
            out.getWarnings().add("Check digit failed for " + label
                    + " — this field was probably misread. Verify it before saving.");
            out.setCheckDigitsValid(false);
        }
    }

    /** ICAO 9303 weighted modulus-10 check digit. */
    public static int checkDigit(String data) {
        int sum = 0, i = 0;
        for (char c : data.toCharArray()) {
            sum += charValue(c) * WEIGHTS[i % 3];
            i++;
        }
        return sum % 10;
    }

    private static int charValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
        return 0;                                    // '<' filler and anything unexpected
    }

    private static PassportScanDTO finish(PassportScanDTO out) {
        out.setSuccess(true);
        out.setIssuingCountryName(CountryCodes.countryName(out.getIssuingCountry()));
        out.setNationalityName(CountryCodes.nationality(out.getNationality()));

        // Gender for the ERP enum.
        out.setGender("F".equals(out.getSex()) ? "FEMALE" : "M".equals(out.getSex()) ? "MALE" : "OTHER");

        // Courtesy title — always overridable in the form.
        out.setSuggestedTitle("F".equals(out.getSex()) ? "Ms" : "M".equals(out.getSex()) ? "Mr" : null);

        // IATA age bands: infant under 2, child 2-11, adult 12+ at time of scan.
        if (out.getDateOfBirth() != null) {
            int age = java.time.Period.between(out.getDateOfBirth(), LocalDate.now()).getYears();
            out.setAge(age);
            out.setSuggestedPassengerType(age < 2 ? "INFANT" : age < 12 ? "CHILD" : "ADULT");
        } else {
            out.setSuggestedPassengerType("ADULT");
        }

        // Six-month validity rule — most destinations refuse boarding inside it.
        if (out.getPassportExpiry() != null) {
            LocalDate sixMonths = LocalDate.now().plusMonths(6);
            if (out.getPassportExpiry().isBefore(LocalDate.now())) {
                out.getWarnings().add("This passport EXPIRED on "
                        + out.getPassportExpiry().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) + ".");
            } else if (out.getPassportExpiry().isBefore(sixMonths)) {
                out.getWarnings().add("This passport expires within six months ("
                        + out.getPassportExpiry().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                        + "). Many destinations will refuse entry.");
            }
        }

        out.setMessage(out.isCheckDigitsValid()
                ? "Passport read successfully — all check digits verified."
                : "Passport read, but some check digits did not match. Please review the highlighted fields.");
        return out;
    }

    // =========================================================================
    // TEXT NORMALISATION
    // =========================================================================

    /**
     * Extracts the plausible MRZ lines from arbitrary OCR output: upper-cases,
     * folds look-alike filler glyphs to '&lt;', drops whitespace and keeps only
     * lines that are long enough and made mostly of the MRZ alphabet.
     */
    private static List<String> candidateLines(String raw) {
        // Normalise every line once, keeping document order — line 1 of the MRZ
        // must stay line 1, so nothing here is allowed to re-sort in place.
        List<String> normalised = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            String s = line.toUpperCase()
                    .replace('«', '<').replace('»', '<').replace('‹', '<').replace('›', '<')
                    .replace('≪', '<').replace('≫', '<').replace('¢', '<')
                    .replace('{', '<').replace('(', '<')
                    .replaceAll("[^A-Z0-9<]", "");
            if (s.length() >= 25) normalised.add(s);
        }
        if (normalised.size() <= 3) return normalised;

        // More than three long lines means OCR also picked up printed page text
        // ("PEOPLE'S REPUBLIC OF BANGLADESH", the emergency-contact block, and
        // so on). Picking the longest lines is NOT good enough — printed text
        // is often long too. Instead, slide a window over consecutive lines and
        // score it on how much it looks like a real MRZ: canonical width, plenty
        // of '<' filler, and — the strongest signal — two or three ADJACENT
        // lines of the same width, which body text almost never produces.
        int bestStart = 0, bestCount = 2, bestScore = Integer.MIN_VALUE;
        for (int count = 3; count >= 2; count--) {
            for (int i = 0; i + count <= normalised.size(); i++) {
                int s = windowScore(normalised, i, count);
                if (s > bestScore) { bestScore = s; bestStart = i; bestCount = count; }
            }
        }
        return new ArrayList<>(normalised.subList(bestStart, bestStart + bestCount));
    }

    /** Averaged MRZ-likeness of {@code count} consecutive lines starting at {@code start}. */
    private static int windowScore(List<String> lines, int start, int count) {
        int total = 0;
        int firstLen = lines.get(start).length();
        for (int i = start; i < start + count; i++) {
            String s = lines.get(i);
            total += lineScore(s);
            if (Math.abs(s.length() - firstLen) <= 2) total += 20;   // uniform width
        }
        // A three-line block only makes sense as TD1 (30 chars); a two-line
        // block as TD3 (44) or TD2 (36). Penalise windows of the wrong shape.
        if (count == 3 && !near(firstLen, 30)) total -= 60;
        if (count == 2 && !(near(firstLen, 44) || near(firstLen, 36))) total -= 30;
        return total / count;   // averaged so 2- and 3-line windows compare fairly
    }

    private static int lineScore(String s) {
        int score = 0;
        int n = s.length();
        if (n == 44 || n == 36 || n == 30) score += 60;
        else if (near(n, 44) || near(n, 36) || near(n, 30)) score += 20;

        long fillers = s.chars().filter(c -> c == '<').count();
        score += (int) Math.min(30, fillers * 100 / Math.max(1, n));

        if (s.contains("<<")) score += 15;     // the ICAO name separator
        return score;
    }

    /** Right-pads (or trims) a line to the exact ICAO length using filler. */
    private static String pad(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + "<".repeat(len - s.length());
    }

    private static boolean near(int actual, int expected) {
        return Math.abs(actual - expected) <= 2;
    }

    /** Folds OCR letter-for-digit errors inside a numeric-only MRZ field. */
    private static String numeric(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) sb.append(TO_DIGIT.getOrDefault(c, c));
        return sb.toString();
    }

    /** Folds OCR digit-for-letter errors inside an alpha-only MRZ field. */
    private static String alpha(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) sb.append(TO_LETTER.getOrDefault(c, c));
        return sb.toString();
    }

    private static String strip(String s) {
        return s == null ? null : s.replace("<", "").trim();
    }

    private static boolean isFillerOnly(String s) {
        return s.chars().allMatch(c -> c == '<');
    }

    /** Convenience for callers that already hold two clean lines. */
    public static PassportScanDTO parse(String line1, String line2) {
        return parse(String.join("\n", Arrays.asList(
                line1 == null ? "" : line1,
                line2 == null ? "" : line2)));
    }
}
