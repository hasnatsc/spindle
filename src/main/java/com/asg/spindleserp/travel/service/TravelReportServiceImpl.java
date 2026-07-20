package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.report.ListReportPdfBuilder;
import com.asg.spindleserp.report.ListReportPdfBuilder.Col;
import com.asg.spindleserp.report.ReportPdfService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.Collections;

/**
 * TravelReportServiceImpl — generates PDF reports using {@link ListReportPdfBuilder}
 * for tabular data and {@link ReportPdfService} for branding and file responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelReportServiceImpl implements TravelReportService {

    private final JdbcTemplate          jdbcTemplate;
    private final TravelBookingService  bookingService;
    private final ListReportPdfBuilder  listPdf;
    private final ReportPdfService      reportPdf;

    // =========================================================================
    // BOOKING CONFIRMATION
    // =========================================================================

    @Override
    public byte[] bookingConfirmation(Long bookingId) {
        TrvBookingDTO b = bookingService.findById(bookingId);
        Long orgId = SecurityHelper.requireOrgId();

        List<String[]> header = new ArrayList<>();
        addKv(header, "Booking No",  b.getBookingNo());
        addKv(header, "Date",        str(b.getBookingDate()));
        addKv(header, "Type",        b.getBookingType());
        addKv(header, "Status",      b.getStatus());
        addKv(header, "Customer",    b.getPartyDisplay());
        addKv(header, "Travel",      str(b.getTravelStartDate()) + " to " + str(b.getTravelEndDate()));

        List<Col> svcCols = List.of(
                Col.text("serviceType", "Type", 14),
                Col.text("description", "Description", 38),
                Col.qty ("quantity",    "Qty",    8, true),
                Col.money("unitPrice",  "Price", 15, false),
                Col.money("lineTotal",  "Total", 15, true)
        );
        List<Map<String, Object>> services = new ArrayList<>();
        if (b.getServices() != null) {
            for (TrvBookingDTO.ServiceLineDTO s : b.getServices()) {
                services.add(mapOf("serviceType", s.getServiceType(), "description", s.getDescription(),
                        "quantity", s.getQuantity(), "unitPrice", s.getUnitPrice(), "lineTotal", s.getLineTotal()));
            }
        }

        List<Col> paxCols = List.of(
                Col.text("name",     "Name",     45),
                Col.text("type",     "Type",     20),
                Col.text("passport", "Passport", 20)
        );
        List<Map<String, Object>> pax = new ArrayList<>();
        if (b.getPassengers() != null) {
            for (TrvBookingDTO.PassengerDTO p : b.getPassengers()) {
                pax.add(mapOf("name", (p.getTitle() != null ? p.getTitle() + " " : "") + p.getFirstName() + " " + str(p.getLastName()),
                        "type", p.getPassengerType(), "passport", str(p.getPassportNumber())));
            }
        }

        List<String[]> footer = new ArrayList<>();
        addAmt(footer, "Total", b.getTotalAmount());
        addAmt(footer, "Paid",  b.getPaidAmount());
        addAmt(footer, "Due",   b.getDueAmount());

        String subtitle = "Booking #" + b.getBookingNo() + " — " + str(b.getBookingDate());
        Map<String, Object> bp = reportPdf.brandingParams(orgId);

        return listPdf.build("Booking Confirmation", subtitle, header, svcCols, services,
                Collections.singletonList(new String[]{"Total (BDT)", fmt(b.getTotalAmount())}), false, bp);
    }

    // =========================================================================
    // AIR TICKET
    // =========================================================================

    @Override
    public byte[] airTicket(Long ticketId) {
        Long orgId = SecurityHelper.requireOrgId();
        Map<String, Object> t = jdbcTemplate.queryForMap("""
            SELECT at.pnr, at.ticket_number, at.validating_carrier, at.fare_basis,
                   at.issue_date, at.fare_amount, at.tax_amount, at.commission_amount,
                   at.service_fee_amount, at.net_fare, at.total_amount,
                   at.agent_vendor_name, at.status, b.booking_no
            FROM trv_air_tickets at
            LEFT JOIN trv_booking_services bs ON bs.id = at.booking_service_id
            LEFT JOIN trv_bookings b ON b.id = bs.booking_id WHERE at.id = ?
            """, ticketId);

        List<String[]> header = new ArrayList<>();
        addKv(header, "PNR",        t.get("pnr"));
        addKv(header, "Ticket No",  t.get("ticket_number"));
        addKv(header, "Carrier",    t.get("validating_carrier"));
        addKv(header, "Fare Basis", t.get("fare_basis"));
        addKv(header, "Issue Date", t.get("issue_date"));
        addKv(header, "Agent",      t.get("agent_vendor_name"));
        addKv(header, "Status",     t.get("status"));

        List<Map<String, Object>> segs = jdbcTemplate.queryForList("""
            SELECT s.flight_number, al.airline_code AS airline,
                   ao.airport_code AS origin_code, ad.airport_code AS dest_code,
                   s.departure_date, s.departure_time, s.arrival_date, s.arrival_time,
                   cc.class_name AS cabin
            FROM trv_air_ticket_segments s
            LEFT JOIN trv_airlines al ON al.id = s.airline_id
            LEFT JOIN trv_airports ao ON ao.id = s.origin_airport_id
            LEFT JOIN trv_airports ad ON ad.id = s.destination_airport_id
            LEFT JOIN trv_cabin_classes cc ON cc.id = s.cabin_class_id
            WHERE s.air_ticket_id = ? ORDER BY s.id
            """, ticketId);

        return listPdf.build("Air Ticket", "PNR: " + str(t.get("pnr")), header,
                List.of(Col.text("flight_number", "Flight", 10), Col.text("airline", "Airline", 12),
                        Col.text("origin_code", "From", 6), Col.text("dest_code", "To", 6),
                        Col.text("departure_date", "Departure", 13), Col.text("arrival_date", "Arrival", 13),
                        Col.text("cabin", "Cabin", 10)),
                segs, Collections.singletonList(new String[]{"Total", "BDT " + fmt(t.get("total_amount"))}), false, reportPdf.brandingParams(orgId));
    }

    // =========================================================================
    // PACKAGE VOUCHER
    // =========================================================================

    @Override
    public byte[] packageVoucher(Long packageBookingId) {
        Long orgId = SecurityHelper.requireOrgId();
        Map<String, Object> pb = jdbcTemplate.queryForMap("""
            SELECT pb.travel_date, pb.pax_count, pb.total_amount, pb.confirmation_number,
                   pb.status, p.package_code, p.package_name, p.destination,
                   p.duration_days, p.duration_nights, p.base_price, p.currency,
                   p.description, p.id AS pkg_id
            FROM trv_package_bookings pb JOIN trv_packages p ON p.id = pb.package_id
            WHERE pb.id = ?
            """, packageBookingId);

        List<String[]> header = new ArrayList<>();
        addKv(header, "Package",   pb.get("package_name"));
        addKv(header, "Code",      pb.get("package_code"));
        addKv(header, "Dest.",     pb.get("destination"));
        addKv(header, "Duration",  pb.get("duration_days") + "D/" + pb.get("duration_nights") + "N");
        addKv(header, "Travel",    pb.get("travel_date"));
        addKv(header, "Pax",       pb.get("pax_count"));
        addKv(header, "Status",    pb.get("status"));

        Long pkgId = ((Number) pb.get("pkg_id")).longValue();
        List<Map<String, Object>> itinerary = jdbcTemplate.queryForList(
            "SELECT day_number, title, description FROM trv_package_itinerary_days WHERE package_id = ? ORDER BY day_number", pkgId);

        return listPdf.build("Package Voucher", str(pb.get("package_name")), header,
                List.of(Col.text("title", "Itinerary Day", 25), Col.text("description", "Details", 60)),
                itinerary, Collections.singletonList(new String[]{"Total", str(pb.get("currency")) + " " + fmt(pb.get("total_amount"))}),
                false, reportPdf.brandingParams(orgId));
    }

    // =========================================================================
    // VISA APPLICATION
    // =========================================================================

    @Override
    public byte[] visaApplication(Long visaId) {
        Long orgId = SecurityHelper.requireOrgId();
        Map<String, Object> va = jdbcTemplate.queryForMap("""
            SELECT va.application_number, va.submission_date, va.expected_date,
                   va.fee_amount, va.status,
                   COALESCE(p.title || ' ' || p.first_name || ' ' || p.last_name, '—') AS passenger_name,
                   vt.country AS visa_country, vt.visa_category, b.booking_no
            FROM trv_visa_applications va
            LEFT JOIN trv_passengers p ON p.id = va.passenger_id
            LEFT JOIN trv_visa_types vt ON vt.id = va.visa_type_id
            LEFT JOIN trv_booking_services bs ON bs.id = va.booking_service_id
            LEFT JOIN trv_bookings b ON b.id = bs.booking_id WHERE va.id = ?
            """, visaId);

        List<String[]> header = new ArrayList<>();
        addKv(header, "Passenger",   va.get("passenger_name"));
        addKv(header, "Visa Type",   str(va.get("visa_country")) + " - " + str(va.get("visa_category")));
        addKv(header, "Booking",     va.get("booking_no"));
        addKv(header, "Application", va.get("application_number"));
        addKv(header, "Submitted",   va.get("submission_date"));
        addKv(header, "Expected",    va.get("expected_date"));
        addKv(header, "Status",      va.get("status"));

        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
            "SELECT document_name, is_received, remarks FROM trv_visa_documents WHERE visa_application_id = ?", visaId);

        return listPdf.build("Visa Application", str(va.get("passenger_name")), header,
                List.of(Col.text("document_name", "Document", 50), Col.text("is_received", "Rcvd", 10),
                        Col.text("remarks", "Remarks", 25)),
                docs, Collections.singletonList(new String[]{"Fee", "BDT " + fmt(va.get("fee_amount"))}),
                false, reportPdf.brandingParams(orgId));
    }

    // =========================================================================
    // REVENUE SUMMARY
    // =========================================================================

    @Override
    public byte[] revenueSummary(String fromDate, String toDate) {
        Long orgId = SecurityHelper.requireOrgId();
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1).toString();
        if (toDate == null)   toDate   = LocalDate.now().toString();

        List<Map<String, Object>> byType = jdbcTemplate.queryForList("""
            SELECT COALESCE(booking_type, 'OTHER') AS label, COUNT(*) AS count,
                   COALESCE(SUM(total_amount), 0) AS revenue
            FROM trv_bookings WHERE organization_id = ? AND status <> 'CANCELLED'
              AND booking_date >= ?::date AND booking_date <= ?::date
            GROUP BY booking_type ORDER BY revenue DESC
            """, orgId, fromDate, toDate);

        return listPdf.build("Revenue Summary", "Period: " + fromDate + " to " + toDate, null,
                List.of(Col.text("label", "Type", 30), Col.qty("count", "Count", 15, true),
                        Col.money("revenue", "Revenue", 30, true)),
                byType, null, false, reportPdf.brandingParams(orgId));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static void addKv(List<String[]> list, String label, Object val) {
        String s = val != null ? val.toString().trim() : "";
        if (!s.isEmpty()) list.add(new String[]{label, s});
    }

    private static void addAmt(List<String[]> list, String label, BigDecimal val) {
        if (val != null) list.add(new String[]{label, "BDT " + String.format("%,.2f", val)});
    }

    private static String str(Object o) { return o != null ? o.toString().trim() : ""; }
    private static String fmt(Object o) {
        if (o == null) return "0.00";
        try { return String.format("%,.2f", new BigDecimal(o.toString())); } catch (Exception e) { return o.toString(); }
    }
    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
