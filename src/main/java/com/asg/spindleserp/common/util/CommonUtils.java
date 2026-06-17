package com.asg.spindleserp.common.util;

import com.asg.spindleserp.common.enums.ApprovalStatus;
import com.asg.spindleserp.security.auth.CustomUserDetails;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CommonUtils
 *
 * Stateless utility class for use across all modules.
 *
 * Security helpers (getCurrentUser, getCurrentOrgId, etc.) are intentionally
 * NOT duplicated here — use {@link SecurityHelper} directly instead:
 *
 *   SecurityHelper.currentOrgId()
 *   SecurityHelper.currentUsername()
 *   SecurityHelper.requireCurrentUser()
 *
 * Document status helpers use {@link ApprovalStatus} (the canonical enum
 * in com.asg.spindleserp.common.enums) rather than the old
 * BusinessDocumentStatus import that no longer exists.
 */
@Component
public class CommonUtils {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ══════════════════════════════════════════════════════════════════════════
    // NULL / BLANK HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    public static String nullSafe(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public static boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SQL SEARCH HELPERS  (used by service datatableList methods)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Builds an " AND (col1 ILIKE '%val%' OR col2 ILIKE '%val%' …) " clause.
     * Returns "" when searchValue is blank (no-op for SQL).
     *
     * NOTE: searchValue is sanitised (single quotes escaped) to prevent
     * trivial SQL injection via the DataTable search box.
     */
    public static String searchILike(String searchValue, List<String> columns) {
        if (isNullOrBlank(searchValue) || columns == null || columns.isEmpty()) {
            return "";
        }
        String safe = searchValue.trim().replace("'", "''");
        return columns.stream()
                .map(col -> col + " ILIKE '%" + safe + "%'")
                .collect(Collectors.joining(" OR ", " AND (", ") "));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENUM HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Converts an enum array to a list of {value, display} maps.
     * Calls getDisplayName() via reflection if available; falls back to name().
     */
    public static List<Map<String, Object>> mapEnumWithDisplay(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(val -> {
                    String display;
                    try {
                        display = (String) val.getClass().getMethod("getDisplayName").invoke(val);
                    } catch (Exception e) {
                        display = formatEnumLabel(val.name());
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("value", val.name());
                    m.put("display", display);
                    return m;
                })
                .toList();
    }

    /**
     * "SOME_ENUM_VALUE" → "Some Enum Value"
     */
    public static String formatEnumLabel(String enumName) {
        if (isNullOrBlank(enumName)) return "";
        return Arrays.stream(enumName.split("_"))
                .map(w -> w.isEmpty() ? w
                        : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String safeEnumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DOCUMENT STATUS HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns true when the document is in a state that should NOT be edited.
     * Editable states: DRAFT, REJECTED, RETURNED.
     */
    public static boolean isNotEditable(ApprovalStatus status) {
        return !EnumSet.of(
                ApprovalStatus.DRAFT,
                ApprovalStatus.REJECTED,
                ApprovalStatus.RETURNED
        ).contains(status);
    }

    public static boolean isRejectedOrReturned(ApprovalStatus status) {
        return status == ApprovalStatus.REJECTED || status == ApprovalStatus.RETURNED;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NUMBER HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    public static double getDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        String s = value.toString().trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static BigDecimal getBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        String s = value.toString().trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATE / DECIMAL FORMAT HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    public static String formatDate(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDate d)      return d.format(DATE_FORMAT);
        if (value instanceof LocalDateTime dt) return dt.toLocalDate().format(DATE_FORMAT);
        return value.toString();
    }

    public static String formatDateTime(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime dt) return dt.format(DATETIME_FORMAT);
        if (value instanceof LocalDate d)      return d.atStartOfDay().format(DATETIME_FORMAT);
        return value.toString();
    }

    public static String formatDecimal(Object value, int scale) {
        BigDecimal bd = getBigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(scale);
        df.setMaximumFractionDigits(scale);
        df.setGroupingUsed(false);
        return df.format(bd);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAP HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Converts a snake_case DB result map to a camelCase map.
     * Useful when returning raw jdbcTemplate rows directly to the frontend.
     */
    public static Map<String, Object> toCamelCaseMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((k, v) -> result.put(snakeToCamel(k), v));
        return result;
    }

    private static String snakeToCamel(String snake) {
        if (snake == null || snake.isEmpty()) return snake;
        String[] parts = snake.split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
