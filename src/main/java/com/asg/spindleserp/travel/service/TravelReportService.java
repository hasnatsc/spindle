package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.*;

import java.util.Map;

/**
 * TravelReportService — JasperReports-based PDF generation for travel documents.
 *
 * Each method returns a PDF byte array that controllers stream to the client.
 */
public interface TravelReportService {

    /** Booking confirmation / invoice. */
    byte[] bookingConfirmation(Long bookingId);

    /** Air ticket / itinerary. */
    byte[] airTicket(Long ticketId);

    /** Package voucher with itinerary and in/exclusions. */
    byte[] packageVoucher(Long packageBookingId);

    /** Visa application summary with document checklist. */
    byte[] visaApplication(Long visaId);

    /** Revenue summary for a date range (default: current month). */
    byte[] revenueSummary(String fromDate, String toDate);
}
