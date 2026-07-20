package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * TravelReportServiceImpl — compiles JRXML templates and fills them with
 * live data from the travel module's existing tables.
 *
 * JRML files are loaded from classpath:/reports/travel/ and compiled
 * once, then cached in memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelReportServiceImpl implements TravelReportService {

    private final JdbcTemplate          jdbcTemplate;
    private final TravelBookingService  bookingService;
    private final ResourceLoader        resourceLoader;

    /** In-memory cache of compiled reports: reportName → JasperReport */
    private final Map<String, JasperReport> compiledCache = new HashMap<>();

    // =========================================================================
    // BOOKING CONFIRMATION
    // =========================================================================

    @Override
    public byte[] bookingConfirmation(Long bookingId) {
        Long orgId = SecurityHelper.requireOrgId();
        TrvBookingDTO booking = bookingService.findById(bookingId);

        Map<String, Object> params = new HashMap<>();
        params.put("title",            "Booking Confirmation");
        params.put("bookingNo",        valueOf(booking.getBookingNo()));
        params.put("bookingDate",      valueOf(booking.getBookingDate()));
        params.put("bookingType",      valueOf(booking.getBookingType()));
        params.put("status",           valueOf(booking.getStatus()));
        params.put("customer",         valueOf(booking.getPartyDisplay()));
        params.put("travelStart",      valueOf(booking.getTravelStartDate()));
        params.put("travelEnd",        valueOf(booking.getTravelEndDate()));
        params.put("totalAmount",      booking.getTotalAmount() != null ? booking.getTotalAmount().toPlainString() : "0");
        params.put("dueAmount",        booking.getDueAmount() != null ? booking.getDueAmount().toPlainString() : "0");
        params.put("paidAmount",       booking.getPaidAmount() != null ? booking.getPaidAmount().toPlainString() : "0");
        params.put("remarks",          valueOf(booking.getRemarks()));
        params.put("orgId",            orgId);

        List<Map<String, Object>> services = new ArrayList<>();
        if (booking.getServices() != null) {
            for (TrvBookingDTO.ServiceLineDTO s : booking.getServices()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("serviceType",  valueOf(s.getServiceType()));
                row.put("description",  valueOf(s.getDescription()));
                row.put("quantity",     s.getQuantity() != null ? s.getQuantity().toPlainString() : "1");
                row.put("unitPrice",    s.getUnitPrice() != null ? s.getUnitPrice().toPlainString() : "0");
                row.put("lineTotal",    s.getLineTotal() != null ? s.getLineTotal().toPlainString() : "0");
                services.add(row);
            }
        }
        params.put("servicesDataSource", new JRBeanCollectionDataSource(services));

        List<Map<String, Object>> passengers = new ArrayList<>();
        if (booking.getPassengers() != null) {
            for (TrvBookingDTO.PassengerDTO p : booking.getPassengers()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name",     valueOf(p.getTitle()) + " " + valueOf(p.getFirstName()) + " " + valueOf(p.getLastName()));
                row.put("type",     valueOf(p.getPassengerType()));
                row.put("passport", valueOf(p.getPassportNumber()));
                passengers.add(row);
            }
        }
        params.put("passengersDataSource", new JRBeanCollectionDataSource(passengers));

        return fillReport("booking-confirmation", params);
    }

    // =========================================================================
    // AIR TICKET
    // =========================================================================

    @Override
    public byte[] airTicket(Long ticketId) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "Air Ticket");

        // Fetch ticket header from DB
        String sql = """
            SELECT at.id, at.pnr, at.ticket_number, at.validating_carrier,
                   at.fare_basis, at.tour_code, at.issue_date,
                   at.fare_amount, at.tax_amount, at.commission_amount,
                   at.service_fee_amount, at.net_fare, at.total_amount,
                   at.agent_vendor_name, at.booking_reference,
                   at.endorsement_restrictions, at.status,
                   b.booking_no
            FROM trv_air_tickets at
            LEFT JOIN trv_booking_services bs ON bs.id = at.booking_service_id
            LEFT JOIN trv_bookings b ON b.id = bs.booking_id
            WHERE at.id = ?
            """;
        Map<String, Object> header = jdbcTemplate.queryForMap(sql, ticketId);
        params.putAll(header);
        params.put("title", "Air Ticket – " + valueOf(header.get("pnr")));

        // Flight segments
        List<Map<String, Object>> segments = jdbcTemplate.queryForList("""
            SELECT s.flight_number,
                   al.airline_code || ' — ' || al.airline_name AS airline_display,
                   ao.airport_code AS origin_code, ad.airport_code AS destination_code,
                   s.departure_date, s.departure_time, s.departure_terminal,
                   s.arrival_date, s.arrival_time, s.arrival_terminal,
                   cc.class_name AS cabin_class, s.baggage_allowance,
                   s.flight_duration_minutes
            FROM trv_air_ticket_segments s
            LEFT JOIN trv_airlines al ON al.id = s.airline_id
            LEFT JOIN trv_airports ao ON ao.id = s.origin_airport_id
            LEFT JOIN trv_airports ad ON ad.id = s.destination_airport_id
            LEFT JOIN trv_cabin_classes cc ON cc.id = s.cabin_class_id
            WHERE s.air_ticket_id = ?
            ORDER BY s.id
            """, ticketId);
        params.put("segmentsDataSource", new JRBeanCollectionDataSource(segments));

        // Passenger tickets
        List<Map<String, Object>> paxTickets = jdbcTemplate.queryForList("""
            SELECT pt.ticket_number, pt.seat_number, pt.baggage_allowance,
                   pt.fare_portion, pt.tax_portion,
                   COALESCE(p.title || ' ' || p.first_name || ' ' || p.last_name, '—') AS passenger_name
            FROM trv_air_ticket_passengers pt
            LEFT JOIN trv_passengers p ON p.id = pt.passenger_id
            WHERE pt.air_ticket_id = ?
            """, ticketId);
        params.put("paxTicketsDataSource", new JRBeanCollectionDataSource(paxTickets));

        return fillReport("air-ticket", params);
    }

    // =========================================================================
    // PACKAGE VOUCHER
    // =========================================================================

    @Override
    public byte[] packageVoucher(Long packageBookingId) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "Package Voucher");

        // Fetch package booking + package details
        String sql = """
            SELECT pb.id, pb.travel_date, pb.pax_count, pb.total_amount,
                   pb.confirmation_number, pb.status,
                   p.package_code, p.package_name, p.destination,
                   p.duration_days, p.duration_nights, p.base_price,
                   p.currency, p.description
            FROM trv_package_bookings pb
            JOIN trv_packages p ON p.id = pb.package_id
            WHERE pb.id = ?
            """;
        Map<String, Object> header = jdbcTemplate.queryForMap(sql, packageBookingId);
        params.putAll(header);
        params.put("title", "Package – " + valueOf(header.get("package_name")));

        // Itinerary days
        Long packageId = (Long) header.get("package_id");
        if (packageId == null && header.get("package_id") instanceof Number) {
            packageId = ((Number) header.get("package_id")).longValue();
        }
        if (packageId != null) {
            List<Map<String, Object>> itinerary = jdbcTemplate.queryForList("""
                SELECT day_number, title, description
                FROM trv_package_itinerary_days
                WHERE package_id = ? ORDER BY day_number
                """, packageId);
            params.put("itineraryDataSource", new JRBeanCollectionDataSource(itinerary));

            List<Map<String, Object>> inclusions = jdbcTemplate.queryForList("""
                SELECT inclusion_type, description
                FROM trv_package_inclusions
                WHERE package_id = ?
                """, packageId);
            params.put("inclusionsDataSource", new JRBeanCollectionDataSource(inclusions));
        }

        return fillReport("package-voucher", params);
    }

    // =========================================================================
    // VISA APPLICATION
    // =========================================================================

    @Override
    public byte[] visaApplication(Long visaId) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "Visa Application");

        String sql = """
            SELECT va.id, va.application_number, va.submission_date,
                   va.expected_date, va.fee_amount, va.status, va.remarks,
                   COALESCE(p.title || ' ' || p.first_name || ' ' || p.last_name, '—') AS passenger_name,
                   vt.country AS visa_country, vt.visa_category, vt.processing_days,
                   b.booking_no
            FROM trv_visa_applications va
            LEFT JOIN trv_passengers p ON p.id = va.passenger_id
            LEFT JOIN trv_visa_types vt ON vt.id = va.visa_type_id
            LEFT JOIN trv_booking_services bs ON bs.id = va.booking_service_id
            LEFT JOIN trv_bookings b ON b.id = bs.booking_id
            WHERE va.id = ?
            """;
        Map<String, Object> header = jdbcTemplate.queryForMap(sql, visaId);
        params.putAll(header);
        params.put("title", "Visa – " + valueOf(header.get("passenger_name")));

        // Document checklist
        List<Map<String, Object>> docs = jdbcTemplate.queryForList("""
            SELECT document_name, is_received, remarks
            FROM trv_visa_documents
            WHERE visa_application_id = ?
            """, visaId);
        params.put("docsDataSource", new JRBeanCollectionDataSource(docs));

        return fillReport("visa-application", params);
    }

    // =========================================================================
    // REVENUE SUMMARY
    // =========================================================================

    @Override
    public byte[] revenueSummary(String fromDate, String toDate) {
        Long orgId = SecurityHelper.requireOrgId();
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1).toString();
        if (toDate == null)   toDate   = LocalDate.now().toString();

        Map<String, Object> params = new HashMap<>();
        params.put("title",     "Revenue Summary");
        params.put("fromDate",  fromDate);
        params.put("toDate",    toDate);
        params.put("orgId",     orgId);

        // Revenue by type
        List<Map<String, Object>> byType = jdbcTemplate.queryForList("""
            SELECT COALESCE(booking_type, 'OTHER') AS label,
                   COUNT(*) AS count,
                   COALESCE(SUM(total_amount), 0) AS revenue
            FROM trv_bookings
            WHERE organization_id = ? AND status <> 'CANCELLED'
              AND booking_date >= ?::date AND booking_date <= ?::date
            GROUP BY booking_type ORDER BY revenue DESC
            """, orgId, fromDate, toDate);
        params.put("byTypeDataSource", new JRBeanCollectionDataSource(byType));

        // Status breakdown
        List<Map<String, Object>> byStatus = jdbcTemplate.queryForList("""
            SELECT status AS label, COUNT(*) AS count,
                   COALESCE(SUM(total_amount), 0) AS revenue
            FROM trv_bookings
            WHERE organization_id = ? AND booking_date >= ?::date AND booking_date <= ?::date
            GROUP BY status ORDER BY count DESC
            """, orgId, fromDate, toDate);
        params.put("byStatusDataSource", new JRBeanCollectionDataSource(byStatus));

        // Top destinations
        List<Map<String, Object>> destinations = jdbcTemplate.queryForList("""
            SELECT destination, SUM(cnt) AS booking_count FROM (
                SELECT COALESCE(h.city, 'Unknown') AS destination, COUNT(*) AS cnt
                FROM trv_hotel_bookings hb JOIN trv_hotels h ON h.id = hb.hotel_id
                WHERE hb.organization_id = ? GROUP BY h.city
                UNION ALL
                SELECT COALESCE(t.destination, 'Unknown'), COUNT(*)
                FROM trv_tour_bookings tb JOIN trv_tours t ON t.id = tb.tour_id
                WHERE tb.organization_id = ? GROUP BY t.destination
                UNION ALL
                SELECT COALESCE(p.destination, 'Unknown'), COUNT(*)
                FROM trv_package_bookings pb JOIN trv_packages p ON p.id = pb.package_id
                WHERE pb.organization_id = ? GROUP BY p.destination
            ) combined
            GROUP BY destination ORDER BY booking_count DESC LIMIT 5
            """, orgId, orgId, orgId);
        params.put("destinationsDataSource", new JRBeanCollectionDataSource(destinations));

        return fillReport("revenue-summary", params);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private byte[] fillReport(String reportName, Map<String, Object> params) {
        try {
            JasperReport jasperReport = compiledCache.computeIfAbsent(reportName, this::compileReport);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate report: {}", reportName, e);
            throw new RuntimeException("Report generation failed: " + e.getMessage(), e);
        }
    }

    private JasperReport compileReport(String reportName) {
        try {
            String path = "classpath:reports/travel/" + reportName + ".jrxml";
            try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
                return JasperCompileManager.compileReport(is);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile report: " + reportName, e);
        }
    }

    private static String valueOf(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }
}
