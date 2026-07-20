package com.asg.spindleserp.travel.controller;

import com.asg.spindleserp.travel.service.TravelReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * TravelReportController — streams PDF reports for the travel module.
 */
@Slf4j
@Controller
@RequestMapping("/travel/reports")
@RequiredArgsConstructor
public class TravelReportController {

    private final TravelReportService reportService;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<byte[]> bookingConfirmation(@PathVariable Long bookingId) {
        byte[] pdf = reportService.bookingConfirmation(bookingId);
        return pdfResponse(pdf, "booking-" + bookingId + ".pdf");
    }

    @GetMapping("/air-ticket/{ticketId}")
    public ResponseEntity<byte[]> airTicket(@PathVariable Long ticketId) {
        byte[] pdf = reportService.airTicket(ticketId);
        return pdfResponse(pdf, "air-ticket-" + ticketId + ".pdf");
    }

    @GetMapping("/package-voucher/{packageBookingId}")
    public ResponseEntity<byte[]> packageVoucher(@PathVariable Long packageBookingId) {
        byte[] pdf = reportService.packageVoucher(packageBookingId);
        return pdfResponse(pdf, "package-voucher-" + packageBookingId + ".pdf");
    }

    @GetMapping("/visa/{visaId}")
    public ResponseEntity<byte[]> visaApplication(@PathVariable Long visaId) {
        byte[] pdf = reportService.visaApplication(visaId);
        return pdfResponse(pdf, "visa-application-" + visaId + ".pdf");
    }

    @GetMapping("/revenue-summary")
    public ResponseEntity<byte[]> revenueSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        byte[] pdf = reportService.revenueSummary(from, to);
        return pdfResponse(pdf, "revenue-summary.pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
