package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.service.StorefrontAuthService;
import com.asg.spindleserp.travel.service.TravelBookingService;
import com.asg.spindleserp.travel.service.TravelDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * TravelPortalAccountController — logged-in travel customer portal.
 *
 * Built on top of the existing eCommerce storefront auth (EcCustomer,
 * SF_CUSTOMER_ID in session), so no separate login system is needed.
 * Travel bookings are linked to the customer via the lead passenger's
 * email or phone on the TrvBooking.
 *
 * Pages (all require login):
 *   GET /travel-portal/account/dashboard
 *   GET /travel-portal/account/bookings
 *   GET /travel-portal/account/bookings/{id}
 *   GET /travel-portal/account/download/{docId}
 *
 * The /travel-portal prefix is permitAll in SecurityConfig and gated
 * by StorefrontAuthInterceptor (registered for /travel-portal/account/**).
 */
@Slf4j
@Controller
@RequestMapping("/travel-portal")
@RequiredArgsConstructor
public class TravelPortalAccountController {

    private final StorefrontAuthService authService;
    private final TravelBookingService  bookingService;
    private final TravelDocumentService documentService;
    private final JdbcTemplate          jdbcTemplate;

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @GetMapping("/account/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/travel-portal/account/dashboard";

        Long orgId = resolveOrgId(request);

        List<Map<String, Object>> bookings = jdbcTemplate.queryForList("""
            SELECT b.id, b.booking_no, b.booking_type, b.total_amount, b.status,
                   TO_CHAR(b.booking_date, 'DD-Mon-YYYY') AS booking_date,
                   TO_CHAR(b.travel_start_date, 'DD-Mon-YYYY') AS travel_start,
                   (SELECT COUNT(*) FROM trv_passengers p WHERE p.booking_id = b.id) AS pax,
                   CASE b.status
                       WHEN 'DRAFT'      THEN 'bg-secondary'
                       WHEN 'CONFIRMED'  THEN 'bg-success'
                       WHEN 'CANCELLED'  THEN 'bg-danger'
                       ELSE 'bg-primary'
                   END AS status_class
            FROM   trv_bookings b
            WHERE  b.organization_id = ?
              AND  (b.id IN (SELECT p.booking_id FROM trv_passengers p
                              WHERE LOWER(p.email) = LOWER(?) OR LOWER(p.phone) = LOWER(?))
                    OR b.remarks ILIKE '%' || ? || '%')
            ORDER  BY b.id DESC LIMIT 10
            """, orgId, customer.getEmail(), customer.getPhone(), customer.getEmail());

        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingCount", bookings.size());
        model.addAttribute("activePage", "travel-portal-dashboard");
        return "travel-site/tp-account-dashboard";
    }

    // =========================================================================
    // BOOKINGS LIST
    // =========================================================================

    @GetMapping("/account/bookings")
    public String bookings(Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null) return "redirect:/account/login?redirect=/travel-portal/account/bookings";

        Long orgId = resolveOrgId(request);

        List<Map<String, Object>> bookings = jdbcTemplate.queryForList("""
            SELECT b.id, b.booking_no, b.booking_type,
                   COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—') AS customer_name,
                   TO_CHAR(b.booking_date, 'DD-Mon-YYYY') AS booking_date,
                   TO_CHAR(b.travel_start_date, 'DD-Mon-YYYY') AS travel_start,
                   TO_CHAR(b.travel_end_date, 'DD-Mon-YYYY') AS travel_end,
                   COALESCE(b.total_amount::text, '0') AS total_amount,
                   COALESCE(b.paid_amount::text, '0')  AS paid_amount,
                   COALESCE(b.due_amount::text, '0')   AS due_amount,
                   CASE b.status
                       WHEN 'DRAFT'      THEN '<span class="badge bg-secondary">Draft</span>'
                       WHEN 'CONFIRMED'  THEN '<span class="badge bg-success">Confirmed</span>'
                       WHEN 'PARTIALLY_PAID' THEN '<span class="badge bg-warning">Partially Paid</span>'
                       WHEN 'PAID'       THEN '<span class="badge bg-primary">Paid</span>'
                       WHEN 'CANCELLED'  THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'COMPLETED'  THEN '<span class="badge bg-dark">Completed</span>'
                       ELSE '<span class="badge bg-info">' || b.status || '</span>'
                   END AS status_badge,
                   (SELECT COUNT(*) FROM trv_passengers p WHERE p.booking_id = b.id) AS pax
            FROM   trv_bookings b
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            WHERE  b.organization_id = ?
              AND  (b.id IN (SELECT p.booking_id FROM trv_passengers p
                              WHERE LOWER(p.email) = LOWER(?) OR LOWER(p.phone) = LOWER(?))
                    OR b.remarks ILIKE '%' || ? || '%')
            ORDER  BY b.id DESC
            """, orgId, customer.getEmail(), customer.getPhone(), customer.getEmail());

        model.addAttribute("customer", customer);
        model.addAttribute("bookings", bookings);
        model.addAttribute("activePage", "travel-portal-bookings");
        return "travel-site/tp-account-bookings";
    }

    // =========================================================================
    // BOOKING DETAIL
    // =========================================================================

    @GetMapping("/account/bookings/{id}")
    public String bookingDetail(@PathVariable Long id, Model model, HttpServletRequest request) {
        EcCustomer customer = authService.currentCustomerOrNull(request);
        if (customer == null)
            return "redirect:/account/login?redirect=/travel-portal/account/bookings/" + id;

        Long orgId = resolveOrgId(request);

        Map<String, Object> booking = jdbcTemplate.queryForMap("""
            SELECT b.*, COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—') AS customer_name
            FROM   trv_bookings b
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            WHERE  b.id = ? AND b.organization_id = ?
            """, id, orgId);

        List<Map<String, Object>> passengers = jdbcTemplate.queryForList("""
            SELECT p.title, p.first_name, p.last_name, p.passport_number,
                   TO_CHAR(p.passport_expiry, 'DD-Mon-YYYY') AS passport_expiry,
                   p.nationality, p.phone, p.email, p.passenger_type,
                   p.is_lead_passenger
            FROM   trv_passengers p
            WHERE  p.booking_id = ?
            ORDER  BY p.is_lead_passenger DESC, p.first_name
            """, id);

        List<Map<String, Object>> services = jdbcTemplate.queryForList("""
            SELECT s.service_type, s.description, s.quantity, s.unit_price, s.line_total
            FROM   trv_booking_services s
            WHERE  s.booking_id = ?
            """, id);

        List<Map<String, Object>> documents = jdbcTemplate.queryForList("""
            SELECT d.id, d.document_type, d.original_file_name, d.file_size_bytes,
                   d.content_type, d.created_at
            FROM   trv_documents d
            WHERE  d.entity_type = 'BOOKING' AND d.entity_id = ?
            ORDER  BY d.created_at DESC
            """, id);

        model.addAttribute("customer", customer);
        model.addAttribute("booking", booking);
        model.addAttribute("passengers", passengers);
        model.addAttribute("services", services);
        model.addAttribute("documents", documents);
        model.addAttribute("activePage", "travel-portal-bookings");
        return "travel-site/tp-account-booking-detail";
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Long resolveOrgId(HttpServletRequest request) {
        try {
            var ctx = com.asg.spindleserp.security.auth.ContextProvider.getOrganizationId();
            if (ctx != null) return ctx;
        } catch (Exception ignored) {}
        // Fallback to the org stored in the customer's session context
        Long orgId = (Long) request.getSession().getAttribute("SF_CUSTOMER_ORG");
        return orgId != null ? orgId : 1L;
    }
}
