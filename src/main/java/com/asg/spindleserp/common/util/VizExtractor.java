package com.asg.spindleserp.common.util;

import com.asg.spindleserp.travel.dto.PassportScanDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VizExtractor — best-effort harvesting of the passport's PRINTED zones.
 *
 * The MRZ does not carry the issue date, issuing authority, place of birth, or
 * anything from the Bangladeshi "Personal Data and Emergency Contact" page
 * (father's / mother's name, permanent address, emergency contact). Those only
 * exist as printed text — the Visual Inspection Zone — so the scanner runs a
 * second, unconstrained OCR pass over the page and this class digs the labelled
 * values out of that raw text.
 *
 * Every regex here was shaped against REAL Tesseract output of a real BGD
 * e-passport photographed at 595px, which is why they tolerate OCR damage such
 * as "Reladonship:" for "Relationship:", "OMPIDHAKA" for "DIP/DHAKA", junk
 * prefixes ("* Name:"), trailing page-edge noise ("AXTHER in"), and a phone
 * number with a space in the middle.
 *
 * IMPORTANT: nothing here is protected by a check digit. Values are pre-fill
 * suggestions only; the MRZ always wins where the two overlap, and callers get
 * a warning telling the operator to verify printed-zone fields by eye.
 */
public final class VizExtractor {

    private VizExtractor() { }

    // ── labels, tolerant of common OCR damage ────────────────────────────────
    private static final Pattern P_FATHER    = Pattern.compile("(?i)father.?s?\\s*name\\s*[:;|=.\\-]*\\s*");
    private static final Pattern P_MOTHER    = Pattern.compile("(?i)mother.?s?\\s*name\\s*[:;|=.\\-]*\\s*");
    private static final Pattern P_PERM_ADDR = Pattern.compile("(?i)perm\\w*\\s*address\\s*[:;|=.\\-]*\\s*");
    private static final Pattern P_EMERGENCY = Pattern.compile("(?i)emergency");
    /** "Reladonship" really happens — hence rela\w*ship. */
    private static final Pattern P_RELATION  = Pattern.compile("(?i)rela\\w*ship\\s*[:;|=.\\-]*\\s*");
    private static final Pattern P_NAME      = Pattern.compile("(?i)\\bname\\s*[:;|=.\\-]*\\s*");
    /** Kinship labels the emergency-contact "Name:" must never match. */
    private static final Pattern P_KIN       = Pattern.compile("(?i)father|mother|guardian");
    private static final Pattern P_PHONE_LBL = Pattern.compile("(?i)tel\\w*|phone");
    private static final Pattern P_PHONE     = Pattern.compile("(\\+?\\d[\\d\\s\\-]{7,20}\\d)");
    private static final Pattern P_AUTHORITY = Pattern.compile("(?i)auth\\w*\\s*[:;|=.\\-]*\\s*");
    private static final Pattern P_POB       = Pattern.compile("(?i)pl?ace\\s*of\\s*b\\w*\\s*[:;|=.\\-]*\\s*");
    /** e.g. BRAHMANBARIA 382110 — district name followed by its 6-digit code. */
    private static final Pattern P_POB_FALLBACK = Pattern.compile("\\b([A-Z]{4,20})\\s+\\d{6}\\b");
    /** Clean DIP/DHAKA, or OCR-merged OMPIDHAKA / D1PIDHAKA style. */
    private static final Pattern P_AUTH_SLASH  = Pattern.compile("\\b([A-Z]{2,5})/([A-Z]{3,15})\\b");
    private static final Pattern P_AUTH_MERGED = Pattern.compile("\\b(DIP|D1P|DlP|OMP|OIP|0IP)\\s*[/1Il]\\s*?([A-Z]{3,15})\\b");
    private static final Pattern P_SURNAME   = Pattern.compile("(?i)surname\\s*[:;|=.\\-]*\\s*");
    private static final Pattern P_GIVEN     = Pattern.compile("(?i)given\\s*name\\w*\\s*[:;|=.\\-]*\\s*");

    private static final Pattern P_DATE_TEXT = Pattern.compile("\\b(\\d{1,2})\\s*([A-Z]{3,4})[A-Z]*\\s*[,.]?\\s*(\\d{4})\\b");
    private static final Pattern P_DATE_NUM  = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b");
    private static final List<String> MONTHS =
            List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Fills the blank printed-zone fields of a scan result from raw VIZ text.
     * MRZ-derived values are never overwritten — with one deliberate
     * exception: when the MRZ name line was flagged unreadable
     * ({@code namesUncertain}), a clearly-printed name is better than a
     * garbled one, so the printed names take over WITH a warning.
     */
    public static void apply(PassportScanDTO dto, String vizText) {
        if (dto == null || vizText == null || vizText.isBlank()) return;
        Map<String, String> f = extract(vizText, dto.getDateOfBirth(), dto.getPassportExpiry());
        if (f.isEmpty()) return;

        boolean touched = false;

        if (dto.getPassportIssueDate() == null && f.containsKey("passportIssueDate")) {
            dto.setPassportIssueDate(LocalDate.parse(f.get("passportIssueDate")));
            touched = true;
        }
        touched |= setIfBlank(dto.getPassportIssuingAuthority(), f.get("passportIssuingAuthority"), dto::setPassportIssuingAuthority);
        touched |= setIfBlank(dto.getPlaceOfBirth(),             f.get("placeOfBirth"),             dto::setPlaceOfBirth);
        touched |= setIfBlank(dto.getFatherName(),               f.get("fatherName"),               dto::setFatherName);
        touched |= setIfBlank(dto.getMotherName(),               f.get("motherName"),               dto::setMotherName);
        touched |= setIfBlank(dto.getPermanentAddress(),         f.get("permanentAddress"),         dto::setPermanentAddress);
        touched |= setIfBlank(dto.getEmergencyContactName(),     f.get("emergencyContactName"),     dto::setEmergencyContactName);
        touched |= setIfBlank(dto.getEmergencyContactRelation(), f.get("emergencyContactRelation"), dto::setEmergencyContactRelation);
        touched |= setIfBlank(dto.getEmergencyContactPhone(),    f.get("emergencyContactPhone"),    dto::setEmergencyContactPhone);

        if (dto.isNamesUncertain()) {
            boolean renamed = false;
            if (present(f.get("surname")))    { dto.setSurname(f.get("surname"));       renamed = true; }
            if (present(f.get("givenNames"))) { dto.setGivenNames(f.get("givenNames")); renamed = true; }
            if (renamed) {
                dto.getWarnings().add("The MRZ name line was unreadable, so the name was taken from the "
                        + "printed page instead — verify it letter by letter.");
                touched = true;
            }
        }

        if (touched) {
            dto.getWarnings().add("Printed-zone fields (issue date, authority, family and emergency details) "
                    + "carry no check digits — they are best-effort OCR. Verify them before saving.");
        }
    }

    /**
     * Pure extraction: raw VIZ text to a field map (dates as ISO strings).
     * {@code dob}/{@code expiry} from the MRZ, when available, anchor the
     * issue-date heuristic and stop the DOB or expiry being mistaken for it.
     */
    public static Map<String, String> extract(String vizText, LocalDate dob, LocalDate expiry) {
        Map<String, String> out = new LinkedHashMap<>();
        if (vizText == null || vizText.isBlank()) return out;

        List<String> lines = new ArrayList<>();
        for (String l : vizText.split("\\r?\\n")) {
            String t = l.trim();
            if (!t.isEmpty()) lines.add(t);
        }
        if (lines.isEmpty()) return out;

        // The personal-data page has TWO "Name:" labels — the holder's, and the
        // emergency contact's. Everything after the EMERGENCY marker belongs to
        // the contact; name labels before it are ignored (the MRZ owns the
        // holder's name).
        // Careful: the PAGE HEADER is "PERSONAL DATA AND EMERGENCY CONTACT" —
        // it contains the word EMERGENCY but is NOT the section marker. The
        // real marker is the "Emergency Contact:" label further down.
        int emergIdx = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String upper = lines.get(i).toUpperCase();
            if (P_EMERGENCY.matcher(upper).find() && !upper.contains("PERSONAL")) { emergIdx = i; break; }
        }

        // ── family block (before EMERGENCY) ──────────────────────────────────
        putName(out, "fatherName", labelValue(lines, 0, emergIdx, P_FATHER));
        putName(out, "motherName", labelValue(lines, 0, emergIdx, P_MOTHER));
        putName(out, "surname",    labelValue(lines, 0, emergIdx, P_SURNAME));
        putName(out, "givenNames", labelValue(lines, 0, emergIdx, P_GIVEN));

        String addr = addressValue(lines, emergIdx);
        if (present(addr)) out.put("permanentAddress", addr);

        // ── emergency block ──────────────────────────────────────────────────
        putName(out, "emergencyContactName",     labelValue(lines, emergIdx, lines.size(), P_NAME, P_KIN));
        putName(out, "emergencyContactRelation", labelValue(lines, emergIdx, lines.size(), P_RELATION));
        String phone = phoneValue(lines, emergIdx);
        if (present(phone)) out.put("emergencyContactPhone", phone);

        // ── data page ────────────────────────────────────────────────────────
        String authority = authorityValue(lines);
        if (present(authority)) out.put("passportIssuingAuthority", authority);

        String pob = pobValue(lines);
        if (present(pob)) out.put("placeOfBirth", pob);

        LocalDate issue = issueDateValue(lines, dob, expiry);
        if (issue != null) out.put("passportIssueDate", issue.toString());

        return out;
    }

    // =========================================================================
    // FIELD STRATEGIES
    // =========================================================================

    /**
     * Finds the first line in [from,to) matching the label; the value is the
     * remainder of that line or, when the label sits alone, the next line.
     */
    private static String labelValue(List<String> lines, int from, int to, Pattern label) {
        return labelValue(lines, from, to, label, null);
    }

    /** As above, skipping any line that matches {@code skip} — used so the
     *  emergency contact's "Name:" never latches onto "Father's Name:". */
    private static String labelValue(List<String> lines, int from, int to, Pattern label, Pattern skip) {
        for (int i = Math.max(0, from); i < Math.min(to, lines.size()); i++) {
            if (skip != null && skip.matcher(lines.get(i)).find()) continue;
            Matcher m = label.matcher(lines.get(i));
            if (!m.find()) continue;
            String rest = lines.get(i).substring(m.end());
            if (uppercasePrefix(rest).length() >= 2) return rest;
            if (i + 1 < to) return lines.get(i + 1);
            return rest;
        }
        return null;
    }

    /** Permanent address: labelled line plus up to two continuation lines. */
    private static String addressValue(List<String> lines, int emergIdx) {
        for (int i = 0; i < emergIdx; i++) {
            Matcher m = P_PERM_ADDR.matcher(lines.get(i));
            if (!m.find()) continue;
            List<String> parts = new ArrayList<>();
            String first = cleanAddress(lines.get(i).substring(m.end()));
            if (present(first)) parts.add(first);
            for (int j = i + 1; j < emergIdx && j <= i + 2; j++) {
                String next = lines.get(j);
                if (P_FATHER.matcher(next).find() || P_MOTHER.matcher(next).find()
                        || P_NAME.matcher(next).find() || P_RELATION.matcher(next).find()) break;
                String cleaned = cleanAddress(next);
                // A continuation looks like address text: mostly capitals/digits.
                if (present(cleaned) && cleaned.replaceAll("[^A-Z0-9]", "").length() >= 4) parts.add(cleaned);
                else break;
            }
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
        return null;
    }

    private static String phoneValue(List<String> lines, int emergIdx) {
        String fallback = null;
        for (int i = emergIdx; i < lines.size(); i++) {
            Matcher m = P_PHONE.matcher(lines.get(i));
            if (!m.find()) continue;
            String normalised = m.group(1).replaceAll("[^+\\d]", "");
            if (normalised.replaceAll("\\D", "").length() < 8) continue;
            if (P_PHONE_LBL.matcher(lines.get(i)).find()) return normalised;   // labelled line wins
            if (fallback == null) fallback = normalised;
        }
        return fallback;
    }

    private static String authorityValue(List<String> lines) {
        // Labelled first, merged-slash fallback second, clean-slash last.
        for (String line : lines) {
            Matcher lbl = P_AUTHORITY.matcher(line);
            if (lbl.find()) {
                String v = normaliseAuthority(line.substring(lbl.end()));
                if (present(v)) return v;
            }
        }
        for (String line : lines) {
            String v = normaliseAuthority(line);
            if (present(v)) return v;
        }
        return null;
    }

    private static String normaliseAuthority(String text) {
        String upper = text.toUpperCase();
        Matcher slash = P_AUTH_SLASH.matcher(upper);
        if (slash.find()) return slash.group(1) + "/" + slash.group(2);
        Matcher merged = P_AUTH_MERGED.matcher(upper);
        if (merged.find()) {
            // BD-specific normalisation: the issuing authority prefix on a
            // Bangladeshi passport is DIP (Department of Immigration and
            // Passports); OMP/OIP/D1P are how OCR mangles it.
            return "DIP/" + merged.group(2);
        }
        return null;
    }

    private static String pobValue(List<String> lines) {
        String labelled = labelValue(lines, 0, lines.size(), P_POB);
        if (labelled != null) {
            String v = titleCase(uppercasePrefix(stripSexPrefix(labelled)));
            if (present(v)) return v;
        }
        for (String line : lines) {
            Matcher m = P_POB_FALLBACK.matcher(line.toUpperCase());
            if (m.find()) return titleCase(m.group(1));
        }
        return null;
    }

    /** "M BRAHMANBARIA" — the sex letter often lands in front of the place. */
    private static String stripSexPrefix(String s) {
        return s.replaceFirst("^\\s*[MFX]\\s+(?=[A-Z]{3})", "");
    }

    /**
     * The issue date has no check digit, but it does have arithmetic: BGD
     * passports run 5 or 10 years, so a past date sitting one validity period
     * before the MRZ expiry IS the issue date, labelled or not.
     */
    private static LocalDate issueDateValue(List<String> lines, LocalDate dob, LocalDate expiry) {
        LocalDate labelled = null, byGap = null;
        for (String line : lines) {
            String upper = line.toUpperCase();
            for (LocalDate d : datesIn(upper)) {
                if (d.equals(dob) || d.equals(expiry) || d.isAfter(LocalDate.now())) continue;
                if (upper.contains("ISSU") && labelled == null) labelled = d;
                if (expiry != null && byGap == null) {
                    long gap = ChronoUnit.DAYS.between(d, expiry);
                    boolean tenYear  = Math.abs(gap - 3653) <= 60;
                    boolean fiveYear = Math.abs(gap - 1827) <= 60;
                    if (tenYear || fiveYear) byGap = d;
                }
            }
        }
        return labelled != null ? labelled : byGap;
    }

    private static List<LocalDate> datesIn(String upperLine) {
        List<LocalDate> out = new ArrayList<>();
        Matcher t = P_DATE_TEXT.matcher(upperLine);
        while (t.find()) {
            int month = MONTHS.indexOf(t.group(2).substring(0, 3)) + 1;
            if (month > 0) addDate(out, Integer.parseInt(t.group(3)), month, Integer.parseInt(t.group(1)));
        }
        Matcher n = P_DATE_NUM.matcher(upperLine);
        while (n.find()) {
            addDate(out, Integer.parseInt(n.group(3)), Integer.parseInt(n.group(2)), Integer.parseInt(n.group(1)));
        }
        return out;
    }

    private static void addDate(List<LocalDate> out, int y, int m, int d) {
        if (y < 1900 || y > 2100) return;
        try { out.add(LocalDate.of(y, m, d)); } catch (Exception ignored) { }
    }

    // =========================================================================
    // CLEANUP HELPERS
    // =========================================================================

    /** Adds a Title-Cased name value when it survives cleanup. */
    private static void putName(Map<String, String> out, String key, String raw) {
        if (raw == null) return;
        String v = titleCase(uppercasePrefix(raw));
        if (v.length() >= 2) out.put(key, v);
    }

    /**
     * Passport names print in CAPITALS, so the value is the leading run of
     * upper-case tokens — which neatly drops trailing page-edge noise such as
     * "AXTHER in" → "AXTHER" or "SPOUSE -—s4" → "SPOUSE".
     */
    private static String uppercasePrefix(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9 .]", " ").trim().replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder();
        for (String token : s.split(" ")) {
            if (token.isEmpty()) continue;
            if (!token.equals(token.toUpperCase()) || !token.matches("[A-Z][A-Z.]*")) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(token);
        }
        return sb.toString();
    }

    private static String cleanAddress(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9 ,./#()\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // Drop stray lowercase junk tokens OCR sprays around the page edges.
        StringBuilder sb = new StringBuilder();
        for (String token : s.split(" ")) {
            if (token.length() <= 3 && token.equals(token.toLowerCase()) && token.matches("[a-z]+")) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(token);
        }
        return sb.toString().replaceAll("^[,\\s\\-]+", "").replaceAll("[,\\s\\-]+$", "");
    }

    private static String titleCase(String words) {
        if (words == null || words.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String w : words.trim().split("\\s+")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.length() == 1 ? w : w.charAt(0) + w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static boolean present(String s) { return s != null && !s.isBlank(); }

    private static boolean setIfBlank(String current, String candidate, java.util.function.Consumer<String> setter) {
        if (present(current) || !present(candidate)) return false;
        setter.accept(candidate);
        return true;
    }
}
