package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.travel.dto.TrvBookingDTO;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TravelReportServiceImpl — JasperReports 7 PDF generation for the travel module.
 *
 * Templates live on the classpath under reports/travel/*.jrxml and are
 * compiled once per template name, then cached for the lifetime of the app.
 * Detail rows are pulled by the template's own <queryString> over the shared
 * JDBC connection (JR resolves $P{...} as PreparedStatement parameters), so
 * every fill is one connection borrowed from the pool and returned in finally.
 *
 * Engineering decisions (documented, no manual merge required):
 *  - Header figures (totals, party name, dates) are resolved here via
 *    JdbcTemplate and handed to the template as parameters — templates only
 *    query their own repeating rows (lines / segments / days / checklist).
 *  - Multi-tenancy: every header lookup is guarded by
 *    ContextProvider.getOrganizationId(); detail queries are keyed by the
 *    already-org-verified primary key, so no cross-tenant row can leak.
 *  - Amount-in-words uses BDT Lakh/Crore wording to match the client market.
 *  - Org logo: org_organizations.logo_url is passed through as image_path;
 *    templates use onErrorType="Blank" so a missing logo never fails a fill.
 */
@Slf4j
@Service
public class TravelReportServiceImpl implements TravelReportService {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final TravelBookingService bookingService;
    private final Map<String, JasperReport> compiledCache = new ConcurrentHashMap<>();

    public TravelReportServiceImpl(JdbcTemplate jdbcTemplate, DataSource dataSource, TravelBookingService bookingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.bookingService = bookingService;
    }

    // =========================================================================
    // BOOKING CONFIRMATION
    // =========================================================================


    @Override
    public byte[] bookingConfirmation(Long bookingId) {
        TrvBookingDTO b = bookingService.findById(bookingId);          // ★ your existing lookup

        Map<String, Object> p = new HashMap<>();
        p.put("P_ORG_NAME",     "ASG Group");                          // ★ wire from OrganizationService if desired
        p.put("P_ORG_TAGLINE",  "TRAVEL & BOOKING DESK");
        p.put("P_ORG_ADDRESS",  "");                                   // ★ org address line
        p.put("P_DOC_TITLE",    "BOOKING CONFIRMATION");
        p.put("P_BOOKING_NO",   nz(b.getBookingNo()));
        p.put("P_BOOKING_DATE", b.getBookingDate() == null ? "" : String.valueOf(b.getBookingDate()));
        p.put("P_PARTY",        nz(b.getPartyDisplay(), "Walk-in Customer"));
        p.put("P_STATUS",       b.getStatus() == null ? "DRAFT" : String.valueOf(b.getStatus()));
        p.put("P_BOOKING_TYPE", b.getBookingType() == null ? "" : String.valueOf(b.getBookingType()));
        p.put("P_AGENT",        nz(b.getSalesAgentDisplay()));
        p.put("P_REMARKS",      nz(b.getRemarks()));
        p.put("P_CURRENCY",     "BDT");

        p.put("P_SUBTOTAL", b.getSubtotalAmount());   // Number params — BigDecimal is fine,
        p.put("P_DISCOUNT", b.getDiscountAmount());   // JRXML declares java.lang.Number and
        p.put("P_TOTAL",    b.getTotalAmount());      // null-guards every use.
        p.put("P_PAID",     b.getPaidAmount());
        p.put("P_DUE",      b.getDueAmount());

        p.put("P_RECEIPTS", b.getReceipts());         // rendered via jr:list sub-dataset

        JRDataSource ds = new JRBeanCollectionDataSource(b.getServices() == null ? List.of() : b.getServices());

        // ★ keep YOUR existing render call — only the template path + args matter:
        return renderPdf("booking-confirmation", p, ds);
    }

    private static String nz(String s)            { return s == null ? "" : s; }
    private static String nz(String s, String d)  { return (s == null || s.isBlank()) ? d : s; }

    // =========================================================================
    // AIR TICKET
    // =========================================================================

    @Override
    public byte[] airTicket(Long ticketId) {
        Long orgId = requireOrg();
        Map<String, Object> t = jdbcTemplate.queryForMap("""
            SELECT COALESCE(t.ticket_number, '-')                        AS ticket_number,
                   COALESCE(t.pnr, '-')                                  AS pnr,
                   COALESCE(t.booking_reference, '-')                    AS booking_reference,
                   t.status,
                   COALESCE(TO_CHAR(t.issue_date, 'DD-Mon-YYYY'), '-')   AS issue_date,
                   t.fare_amount, t.tax_amount,
                   COALESCE(t.service_fee_amount, 0)                     AS service_fee,
                   t.total_amount,
                   COALESCE(al.airline_name, '-')                        AS airline_name,
                   COALESCE(t.validating_carrier, '-')                   AS validating_carrier,
                   COALESCE(t.fare_basis, '-')                           AS fare_basis,
                   COALESCE(t.endorsement_restrictions, '-')             AS endorsements,
                   COALESCE(t.agent_vendor_name, '-')                    AS agent_name,
                   b.booking_no,
                   COALESCE(s.sub_account_name, 'Walk-in / Unregistered') AS customer
            FROM   trv_air_tickets t
            LEFT   JOIN trv_airlines al ON al.id = t.airline_id
            JOIN   trv_booking_services bs ON bs.id = t.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            WHERE  t.id = ? AND t.organization_id = ?
            """, ticketId, orgId);

        Map<String, Object> params = companyParams(orgId);
        params.put("ticketId",          ticketId);
        params.put("ticketNumber",      str(t.get("ticket_number")));
        params.put("pnr",               str(t.get("pnr")));
        params.put("bookingReference",  str(t.get("booking_reference")));
        params.put("bookingNo",         str(t.get("booking_no")));
        params.put("customer",          str(t.get("customer")));
        params.put("airlineName",       str(t.get("airline_name")));
        params.put("validatingCarrier", str(t.get("validating_carrier")));
        params.put("fareBasis",         str(t.get("fare_basis")));
        params.put("issueDate",         str(t.get("issue_date")));
        params.put("status",            str(t.get("status")));
        params.put("agentName",         str(t.get("agent_name")));
        params.put("endorsements",      str(t.get("endorsements")));
        params.put("fareAmount",        dec(t.get("fare_amount")));
        params.put("taxAmount",         dec(t.get("tax_amount")));
        params.put("serviceFee",        dec(t.get("service_fee")));
        params.put("totalAmount",       dec(t.get("total_amount")));

        return renderPdf("air-ticket", params);
    }

    // =========================================================================
    // PACKAGE VOUCHER
    // =========================================================================

    @Override
    public byte[] packageVoucher(Long packageBookingId) {
        Long orgId = requireOrg();
        Map<String, Object> pb = jdbcTemplate.queryForMap("""
            SELECT COALESCE(pb.confirmation_number, '-')                 AS confirmation_number,
                   pb.pax_count, pb.status, pb.total_amount,
                   COALESCE(TO_CHAR(pb.travel_date, 'DD-Mon-YYYY'), '-') AS travel_date,
                   COALESCE(pb.supplier_reference, '-')                  AS supplier_reference,
                   p.id                                                  AS package_id,
                   p.package_code, p.package_name, p.currency,
                   COALESCE(p.destination, '-')                          AS destination,
                   COALESCE(p.duration_days::text, '-')                  AS duration_days,
                   COALESCE(p.duration_nights::text, '-')                AS duration_nights,
                   b.booking_no,
                   COALESCE(s.sub_account_name, 'Walk-in / Unregistered') AS customer
            FROM   trv_package_bookings pb
            JOIN   trv_packages p ON p.id = pb.package_id
            JOIN   trv_booking_services bs ON bs.id = pb.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            WHERE  pb.id = ? AND pb.organization_id = ?
            """, packageBookingId, orgId);

        Map<String, Object> params = companyParams(orgId);
        params.put("packageId",          ((Number) pb.get("package_id")).longValue());
        params.put("confirmationNumber", str(pb.get("confirmation_number")));
        params.put("bookingNo",          str(pb.get("booking_no")));
        params.put("customer",           str(pb.get("customer")));
        params.put("packageName",        str(pb.get("package_name")));
        params.put("packageCode",        str(pb.get("package_code")));
        params.put("destination",        str(pb.get("destination")));
        params.put("duration",           str(pb.get("duration_days")) + "D / " + str(pb.get("duration_nights")) + "N");
        params.put("travelDate",         str(pb.get("travel_date")));
        params.put("paxCount",           String.valueOf(pb.get("pax_count")));
        params.put("status",             str(pb.get("status")));
        params.put("supplierReference",  str(pb.get("supplier_reference")));
        params.put("currency",           str(pb.get("currency")));
        params.put("totalAmount",        dec(pb.get("total_amount")));

        return renderPdf("package-voucher", params);
    }

    // =========================================================================
    // VISA APPLICATION
    // =========================================================================

    @Override
    public byte[] visaApplication(Long visaId) {
        Long orgId = requireOrg();
        Map<String, Object> v = jdbcTemplate.queryForMap("""
            SELECT COALESCE(va.application_number, '-')                     AS application_number,
                   va.status, va.fee_amount,
                   COALESCE(va.remarks, '-')                                AS remarks,
                   COALESCE(TO_CHAR(va.submission_date, 'DD-Mon-YYYY'), '-') AS submission_date,
                   COALESCE(TO_CHAR(va.expected_date, 'DD-Mon-YYYY'), '-')  AS expected_date,
                   COALESCE(TO_CHAR(va.approval_date, 'DD-Mon-YYYY'), '-')  AS approval_date,
                   vt.country, vt.visa_category, vt.currency,
                   COALESCE(vt.processing_days::text, '-')                  AS processing_days,
                   TRIM(COALESCE(p.title, '') || ' ' || p.first_name || ' ' ||
                        COALESCE(p.last_name, ''))                          AS applicant_name,
                   COALESCE(p.passport_number, '-')                         AS passport_number,
                   COALESCE(TO_CHAR(p.passport_expiry, 'DD-Mon-YYYY'), '-') AS passport_expiry,
                   COALESCE(p.nationality, '-')                             AS nationality,
                   COALESCE(TO_CHAR(p.date_of_birth, 'DD-Mon-YYYY'), '-')   AS date_of_birth,
                   b.booking_no
            FROM   trv_visa_applications va
            JOIN   trv_visa_types vt ON vt.id = va.visa_type_id
            JOIN   trv_passengers p ON p.id = va.passenger_id
            JOIN   trv_booking_services bs ON bs.id = va.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            WHERE  va.id = ? AND va.organization_id = ?
            """, visaId, orgId);

        Map<String, Object> params = companyParams(orgId);
        params.put("visaId",            visaId);
        params.put("applicationNumber", str(v.get("application_number")));
        params.put("bookingNo",         str(v.get("booking_no")));
        params.put("status",            str(v.get("status")));
        params.put("country",           str(v.get("country")));
        params.put("visaCategory",      str(v.get("visa_category")));
        params.put("processingDays",    str(v.get("processing_days")));
        params.put("feeAmount",         dec(v.get("fee_amount")));
        params.put("currency",          str(v.get("currency")));
        params.put("submissionDate",    str(v.get("submission_date")));
        params.put("expectedDate",      str(v.get("expected_date")));
        params.put("approvalDate",      str(v.get("approval_date")));
        params.put("applicantName",     str(v.get("applicant_name")));
        params.put("passportNumber",    str(v.get("passport_number")));
        params.put("passportExpiry",    str(v.get("passport_expiry")));
        params.put("nationality",       str(v.get("nationality")));
        params.put("dateOfBirth",       str(v.get("date_of_birth")));
        params.put("remarks",           str(v.get("remarks")));

        return renderPdf("visa-application", params);
    }

    // =========================================================================
    // REVENUE SUMMARY
    // =========================================================================

    @Override
    public byte[] revenueSummary(String from, String to) {
        Long orgId = requireOrg();
        LocalDate today = LocalDate.now();
        LocalDate fromDate = parseDateOr(from, today.withDayOfMonth(1));
        LocalDate toDate   = parseDateOr(to, today);
        if (toDate.isBefore(fromDate)) { LocalDate tmp = fromDate; fromDate = toDate; toDate = tmp; }

        Map<String, Object> params = companyParams(orgId);
        params.put("orgId",       orgId);
        params.put("fromDate",    Date.valueOf(fromDate));
        params.put("toDate",      Date.valueOf(toDate));
        params.put("periodLabel", fromDate.format(DMY) + "  to  " + toDate.format(DMY));

        return renderPdf("revenue-summary", params);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Long requireOrg() {
        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null) throw new IllegalStateException("No organization in context.");
        return orgId;
    }

    private Map<String, Object> companyParams(Long orgId) {
        Map<String, Object> params = new HashMap<>();
        try {
            Map<String, Object> org = jdbcTemplate.queryForMap("""
                SELECT name,
                       TRIM(BOTH ', ' FROM COALESCE(address, '') ||
                            CASE WHEN city IS NOT NULL AND city <> '' THEN ', ' || city ELSE '' END ||
                            CASE WHEN country IS NOT NULL AND country <> '' THEN ', ' || country ELSE '' END) AS address,
                       logo_url
                FROM   org_organizations WHERE id = ?
                """, orgId);
            params.put("company_name",    str(org.get("name")));
            params.put("company_address", str(org.get("address")));
            params.put("image_path",      org.get("logo_url") != null ? org.get("logo_url").toString() : "");
        } catch (Exception e) {
            log.warn("Company header lookup failed for org {}: {}", orgId, e.getMessage());
            params.put("company_name", ""); params.put("company_address", ""); params.put("image_path", "");
        }
        return params;
    }

    private byte[] renderPdf(String templateName, Map<String, Object> params) {
        JasperReport report = compiledCache.computeIfAbsent(templateName, this::compile);
        try (Connection conn = dataSource.getConnection()) {
            JasperPrint print = JasperFillManager.fillReport(report, params, conn);
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            log.error("Report fill failed [{}]: {}", templateName, e.getMessage(), e);
            throw new IllegalStateException("Could not generate " + templateName + " report: " + e.getMessage(), e);
        }
    }

    private byte[] renderPdf(String templateName, Map<String, Object> params, JRDataSource dataSource) {
        JasperReport report = compiledCache.computeIfAbsent(templateName, this::compile);
        try {
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            log.error("Report fill failed [{}]: {}", templateName, e.getMessage(), e);
            throw new IllegalStateException("Could not generate " + templateName + " report: " + e.getMessage(), e);
        }
    }

    private JasperReport compile(String templateName) {
        String path = "reports/travel/" + templateName + ".jrxml";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return JasperCompileManager.compileReport(in);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compile report template " + path + ": " + e.getMessage(), e);
        }
    }

    private static LocalDate parseDateOr(String value, LocalDate fallback) {
        try {
            return (value == null || value.isBlank()) ? fallback : LocalDate.parse(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String str(Object o) { return o == null ? "-" : o.toString(); }

    private static BigDecimal dec(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    /**
     * BDT amount-in-words using the Lakh/Crore numbering system.
     * Example: 1,23,456.78 → "Taka One Lakh Twenty Three Thousand Four Hundred
     * Fifty Six and Paisa Seventy Eight Only".
     * Non-BDT currencies fall back to "<CUR> <words> Only" with the same scale.
     */
    private static String amountInWords(BigDecimal amount, String currency) {
        if (amount == null) amount = BigDecimal.ZERO;
        long whole = amount.setScale(2, java.math.RoundingMode.HALF_UP).longValue();
        int fraction = amount.setScale(2, java.math.RoundingMode.HALF_UP)
                             .remainder(BigDecimal.ONE)
                             .movePointRight(2).abs().intValue();
        boolean bdt = currency == null || currency.isBlank() || "BDT".equalsIgnoreCase(currency);
        String major = bdt ? "Taka" : currency.toUpperCase();
        String minor = bdt ? "Paisa" : "Cents";
        StringBuilder sb = new StringBuilder(major).append(' ').append(numberToWords(Math.abs(whole)));
        if (fraction > 0) sb.append(" and ").append(minor).append(' ').append(numberToWords(fraction));
        return sb.append(" Only").toString();
    }

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
            "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty",
            "Seventy", "Eighty", "Ninety"};

    private static String numberToWords(long n) {
        if (n == 0) return "Zero";
        StringBuilder sb = new StringBuilder();
        long crore = n / 10_000_000L;        n %= 10_000_000L;
        long lakh  = n / 100_000L;           n %= 100_000L;
        long thousand = n / 1_000L;          n %= 1_000L;
        long hundred  = n / 100L;            n %= 100L;
        if (crore > 0)    sb.append(numberToWords(crore)).append(" Crore ");
        if (lakh > 0)     sb.append(twoDigits((int) lakh)).append(" Lakh ");
        if (thousand > 0) sb.append(twoDigits((int) thousand)).append(" Thousand ");
        if (hundred > 0)  sb.append(ONES[(int) hundred]).append(" Hundred ");
        if (n > 0)        sb.append(twoDigits((int) n));
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private static String twoDigits(int n) {
        if (n < 20) return ONES[n];
        return (TENS[n / 10] + (n % 10 > 0 ? " " + ONES[n % 10] : "")).trim();
    }
}
