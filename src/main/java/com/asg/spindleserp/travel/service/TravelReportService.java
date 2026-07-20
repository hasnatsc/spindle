package com.asg.spindleserp.travel.service;

/**
 * TravelReportService — produces PDF reports for the travel module.
 * Each method returns the rendered PDF bytes (streamed inline by
 * TravelReportController).
 */
public interface TravelReportService {

    /** Booking confirmation with service lines + passenger manifest. */
    byte[] bookingConfirmation(Long bookingId);

    /** E-ticket / itinerary receipt with flight segments + per-pax tickets. */
    byte[] airTicket(Long ticketId);

    /** Package service voucher with day-wise itinerary + inclusions/exclusions. */
    byte[] packageVoucher(Long packageBookingId);

    /** Visa application status sheet with document checklist. */
    byte[] visaApplication(Long visaId);

    /**
     * Revenue summary for a date range (yyyy-MM-dd strings, both optional —
     * defaults to the current month-to-date), grouped by booking type.
     */
    byte[] revenueSummary(String from, String to);
}
