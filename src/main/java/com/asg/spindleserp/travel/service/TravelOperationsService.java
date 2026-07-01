package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvAirTicketDTO;
import com.asg.spindleserp.travel.dto.TrvHotelBookingDTO;
import com.asg.spindleserp.travel.dto.TrvSupplierCostDTO;

import java.util.List;
import java.util.Map;

/**
 * TravelOperationsService — fulfillment-side entities that hang off a
 * confirmed booking's service lines: hotel bookings, air tickets
 * (+ passenger tickets), and supplier costs.
 */
public interface TravelOperationsService {

    // ── Hotel Bookings ────────────────────────────────────────────────────────
    TrvHotelBookingDTO saveHotelBooking(TrvHotelBookingDTO dto);
    TrvHotelBookingDTO findHotelBookingById(Long id);
    void deleteHotelBooking(Long id);
    TrvHotelBookingDTO changeHotelBookingStatus(Long id, String status);
    List<Map<String, Object>> listHotelBookings(String search);

    // ── Air Tickets ───────────────────────────────────────────────────────────
    TrvAirTicketDTO saveAirTicket(TrvAirTicketDTO dto);
    TrvAirTicketDTO findAirTicketById(Long id);
    void deleteAirTicket(Long id);
    TrvAirTicketDTO changeAirTicketStatus(Long id, String status);
    List<Map<String, Object>> listAirTickets(String search);

    // ── Supplier Costs ────────────────────────────────────────────────────────
    TrvSupplierCostDTO saveSupplierCost(TrvSupplierCostDTO dto);
    void deleteSupplierCost(Long id);
    List<Map<String, Object>> listSupplierCosts(String search);

    // ── Lookups ───────────────────────────────────────────────────────────────
    /** Booking services (lines) still needing hotel/air fulfillment, filtered by type. */
    List<Map<String, Object>> unfulfilledServiceLines(String serviceType);

    /** Passengers on the booking that owns the given booking-service line. */
    List<Map<String, Object>> passengersForServiceLine(Long bookingServiceId);
}
