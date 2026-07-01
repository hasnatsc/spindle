package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvTourBookingDTO;
import com.asg.spindleserp.travel.dto.TrvTourDTO;
import com.asg.spindleserp.travel.dto.TrvTourGuideDTO;

import java.util.List;
import java.util.Map;

public interface TravelTourService {

    // ── Tours ─────────────────────────────────────────────────────────────────
    TrvTourDTO saveTour(TrvTourDTO dto);
    TrvTourDTO findTourById(Long id);
    void deleteTour(Long id);
    List<Map<String, Object>> listTours(String search);
    List<Map<String, Object>> searchTours(String search); // Select2

    // ── Tour Guides (lookup) ─────────────────────────────────────────────────
    TrvTourGuideDTO saveGuide(TrvTourGuideDTO dto);
    void deleteGuide(Long id);
    List<Map<String, Object>> listGuides();
    List<Map<String, Object>> searchGuides(); // Select2

    // ── Tour Bookings (fulfillment for TOUR service lines) ──────────────────
    TrvTourBookingDTO saveTourBooking(TrvTourBookingDTO dto);
    TrvTourBookingDTO findTourBookingById(Long id);
    void deleteTourBooking(Long id);
    TrvTourBookingDTO changeTourBookingStatus(Long id, String status);
    List<Map<String, Object>> listTourBookings(String search);
}
