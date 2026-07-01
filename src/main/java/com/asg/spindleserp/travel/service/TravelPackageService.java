package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.TrvPackageBookingDTO;
import com.asg.spindleserp.travel.dto.TrvPackageDTO;

import java.util.List;
import java.util.Map;

public interface TravelPackageService {

    // ── Packages (with nested itinerary days + inclusions) ──────────────────
    TrvPackageDTO savePackage(TrvPackageDTO dto);
    TrvPackageDTO findPackageById(Long id);
    void deletePackage(Long id);
    List<Map<String, Object>> listPackages(String search);
    List<Map<String, Object>> searchPackages(String search); // Select2

    // ── Package Bookings (fulfillment for PACKAGE service lines) ────────────
    TrvPackageBookingDTO savePackageBooking(TrvPackageBookingDTO dto);
    TrvPackageBookingDTO findPackageBookingById(Long id);
    void deletePackageBooking(Long id);
    TrvPackageBookingDTO changePackageBookingStatus(Long id, String status);
    List<Map<String, Object>> listPackageBookings(String search);
}
