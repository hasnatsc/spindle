package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.travel.dto.PassportScanDTO;
import com.asg.spindleserp.travel.dto.TrvPassengerDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * TravelPassengerService — standalone passenger CRUD plus passport reading.
 *
 * Kept separate from TravelBookingService: that one owns the nested
 * booking-save path (syncPassengers / syncPreferencesAfterSave) and must not
 * change. This service is the single-row path used by the Passengers screen
 * and by the New Passenger modal on Air Tickets.
 */
public interface TravelPassengerService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    TrvPassengerDTO save(TrvPassengerDTO dto);

    TrvPassengerDTO findById(Long id);

    void delete(Long id);

    /** DataTable-shaped rows for the passenger list, org-scoped. */
    List<Map<String, Object>> list(String search, Long bookingId, String expiryFilter);

    /** Marks one passenger as the booking's lead and clears the flag on the rest. */
    TrvPassengerDTO setLeadPassenger(Long id);

    // ── Lookups for the Select2 boxes ────────────────────────────────────────

    /** Bookings the operator can attach a passenger to. */
    List<Map<String, Object>> bookingSearch(String q);

    /** Passengers on the booking that owns a given booking-service line. */
    List<Map<String, Object>> passengersForServiceLine(Long bookingServiceId);

    /** Creates a passenger against the booking behind a service line (Air Tickets modal). */
    TrvPassengerDTO createForServiceLine(Long bookingServiceId, TrvPassengerDTO dto);

    // ── Passport reading ──────────────────────────────────────────────────────

    /**
     * Parses MRZ text into passenger fields. This is the workhorse — it is
     * what the browser calls after client-side OCR, and it is also what runs
     * when an operator types the two bottom lines by hand.
     */
    PassportScanDTO parseMrz(String mrzText);

    /**
     * As above, plus best-effort harvesting of the printed page (VIZ): issue
     * date, issuing authority, place of birth, father's / mother's name,
     * permanent address and emergency contact. {@code vizText} is the raw
     * output of an unconstrained OCR pass and may be null.
     */
    PassportScanDTO parseMrz(String mrzText, String vizText);

    /**
     * Printed-zone extraction on its own — the browser calls this after the
     * MRZ result is already on screen, so the slow full-page pass never
     * delays the check-digit-verified fields. Dates are ISO strings.
     */
    Map<String, String> extractViz(String vizText, String dateOfBirth, String passportExpiry);

    /**
     * Server-side scan of an uploaded passport image. Only works when a
     * PassportOcrEngine bean is registered; otherwise returns success=false
     * with a message telling the UI to use browser OCR instead.
     */
    PassportScanDTO scanImage(MultipartFile file);

    /** True when a server-side OCR engine is available — drives the UI hint. */
    boolean serverOcrAvailable();

    /**
     * Stores the passport image against the passenger via TravelDocumentService
     * and stamps trv_passengers.passport_document_id. Returns the document id.
     */
    Long attachPassportImage(Long passengerId, MultipartFile file);
}
