package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.common.util.MrzParser;
import com.asg.spindleserp.common.util.VizExtractor;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.PassportScanDTO;
import com.asg.spindleserp.travel.dto.TrvDocumentDTO;
import com.asg.spindleserp.travel.dto.TrvPassengerDTO;
import com.asg.spindleserp.travel.entity.TrvBooking;
import com.asg.spindleserp.travel.entity.TrvPassenger;
import com.asg.spindleserp.travel.entity.TrvPassengerPreference;
import com.asg.spindleserp.travel.repository.TrvBookingRepository;
import com.asg.spindleserp.travel.repository.TrvPassengerPreferenceRepository;
import com.asg.spindleserp.travel.repository.TrvPassengerRepository;
import com.asg.spindleserp.travel.repository.TrvPassengerTicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class TravelPassengerServiceImpl implements TravelPassengerService {

    private final TrvPassengerRepository           passengerRepo;
    private final TrvPassengerPreferenceRepository preferenceRepo;
    private final TrvPassengerTicketRepository     ticketRepo;
    private final TrvBookingRepository             bookingRepo;
    private final TravelDocumentService            documentService;
    private final JdbcTemplate                     jdbcTemplate;

    /**
     * ★ Empty by default. Register any @Service implementing PassportOcrEngine
     * and server-side scanning switches on with no other change.
     */
    private final Optional<PassportOcrEngine> ocrEngine;

    public TravelPassengerServiceImpl(TrvPassengerRepository passengerRepo,
                                      TrvPassengerPreferenceRepository preferenceRepo,
                                      TrvPassengerTicketRepository ticketRepo,
                                      TrvBookingRepository bookingRepo,
                                      TravelDocumentService documentService,
                                      JdbcTemplate jdbcTemplate,
                                      Optional<PassportOcrEngine> ocrEngine) {
        this.passengerRepo   = passengerRepo;
        this.preferenceRepo  = preferenceRepo;
        this.ticketRepo      = ticketRepo;
        this.bookingRepo     = bookingRepo;
        this.documentService = documentService;
        this.jdbcTemplate    = jdbcTemplate;
        this.ocrEngine       = ocrEngine;
    }

    // =========================================================================
    // SAVE  (create + update)
    // =========================================================================

    @Override
    public TrvPassengerDTO save(TrvPassengerDTO dto) {
        Long orgId = SecurityHelper.requireOrgId();

        TrvPassenger e;
        if (dto.getId() != null) {
            e = passengerRepo.findByIdAndOrg(dto.getId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger #" + dto.getId() + " not found."));
        } else {
            if (dto.getBookingId() == null)
                throw new IllegalArgumentException("Select the booking this passenger belongs to.");
            TrvBooking booking = bookingRepo.findById(dto.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking #" + dto.getBookingId() + " not found."));
            // Tenant guard — never let a client-supplied booking id cross orgs.
            if (booking.getOrganization() == null || !orgId.equals(booking.getOrganization().getId()))
                throw new IllegalArgumentException("That booking belongs to another organization.");
            e = new TrvPassenger();
            e.setBooking(booking);
        }

        if (dto.getFirstName() == null || dto.getFirstName().isBlank())
            throw new IllegalArgumentException("First name is required.");

        // Duplicate passport on the same booking is almost always a double entry.
        String passportNo = trimUpper(dto.getPassportNumber());
        long excludeId = e.getId() != null ? e.getId() : -1L;
        if (passportNo != null && !passportNo.isBlank()
                && passengerRepo.countDuplicatePassport(e.getBooking().getId(), passportNo, excludeId) > 0)
            throw new IllegalArgumentException(
                "Passport " + passportNo + " is already on this booking.");

        // ── Identity ──────────────────────────────────────────────────────────
        e.setTitle(trim(dto.getTitle()));
        e.setFirstName(trim(dto.getFirstName()));
        e.setLastName(trim(dto.getLastName()));
        e.setDateOfBirth(dto.getDateOfBirth());
        e.setGender(dto.getGender() != null && !dto.getGender().isBlank()
                ? TrvPassenger.Gender.valueOf(dto.getGender()) : null);
        e.setPassengerType(dto.getPassengerType() != null && !dto.getPassengerType().isBlank()
                ? TrvPassenger.PassengerType.valueOf(dto.getPassengerType())
                : TrvPassenger.PassengerType.ADULT);

        // ── Contact ───────────────────────────────────────────────────────────
        e.setPhone(trim(dto.getPhone()));
        e.setEmail(trim(dto.getEmail()));
        e.setRemarks(trim(dto.getRemarks()));

        // ── Passport ──────────────────────────────────────────────────────────
        e.setPassportNumber(passportNo);
        e.setPassportExpiry(dto.getPassportExpiry());
        e.setPassportIssueDate(dto.getPassportIssueDate());
        e.setNationality(trim(dto.getNationality()));
        e.setPassportCountry(trimUpper(dto.getPassportCountry()));
        e.setPassportIssuingAuthority(trim(dto.getPassportIssuingAuthority()));
        e.setPersonalNumber(trim(dto.getPersonalNumber()));
        e.setPlaceOfBirth(trim(dto.getPlaceOfBirth()));

        // ── Personal data page ────────────────────────────────────────────────
        e.setFatherName(trim(dto.getFatherName()));
        e.setMotherName(trim(dto.getMotherName()));
        e.setPermanentAddress(trim(dto.getPermanentAddress()));
        e.setEmergencyContactName(trim(dto.getEmergencyContactName()));
        e.setEmergencyContactRelation(trim(dto.getEmergencyContactRelation()));
        e.setEmergencyContactPhone(trim(dto.getEmergencyContactPhone()));

        // ── Scan audit ────────────────────────────────────────────────────────
        e.setMrzLine1(trim(dto.getMrzLine1()));
        e.setMrzLine2(trim(dto.getMrzLine2()));
        e.setMrzLine3(trim(dto.getMrzLine3()));
        if (dto.getPassportDocumentId() != null) e.setPassportDocumentId(dto.getPassportDocumentId());

        // ── Lead flag ─────────────────────────────────────────────────────────
        boolean wantsLead = Boolean.TRUE.equals(dto.getIsLeadPassenger());
        e.setIsLeadPassenger(wantsLead);

        String user = SecurityHelper.currentUsername().orElse("system");
        if (e.getCreatedBy() == null) e.setCreatedBy(user);
        e.setUpdatedBy(user);

        TrvPassenger saved = passengerRepo.save(e);

        // Exactly one lead per booking.
        if (wantsLead) {
            List<TrvPassenger> others =
                passengerRepo.findOtherLeads(saved.getBooking().getId(), saved.getId());
            others.forEach(o -> o.setIsLeadPassenger(false));
            if (!others.isEmpty()) passengerRepo.saveAll(others);
        }

        savePreference(dto, saved.getId());
        return toDTO(saved);
    }

    private void savePreference(TrvPassengerDTO dto, Long passengerId) {
        TrvPassengerDTO.PreferenceDTO p = dto.getPreference();
        if (p == null) return;
        boolean allBlank = blank(p.getMealPreference()) && blank(p.getSeatPreference())
                && blank(p.getSpecialAssistance()) && blank(p.getDietaryRestriction())
                && blank(p.getRemarks());

        Optional<TrvPassengerPreference> existing = preferenceRepo.findByPassengerId(passengerId);
        if (allBlank) {
            existing.ifPresent(pref -> preferenceRepo.deleteById(pref.getId()));
            return;
        }
        TrvPassengerPreference pref = existing.orElseGet(() ->
            TrvPassengerPreference.builder().passengerId(passengerId).build());
        pref.setMealPreference(trim(p.getMealPreference()));
        pref.setSeatPreference(trim(p.getSeatPreference()));
        pref.setSpecialAssistance(trim(p.getSpecialAssistance()));
        pref.setDietaryRestriction(trim(p.getDietaryRestriction()));
        pref.setRemarks(trim(p.getRemarks()));
        preferenceRepo.save(pref);
    }

    // =========================================================================
    // FIND / DELETE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public TrvPassengerDTO findById(Long id) {
        TrvPassenger e = passengerRepo.findByIdAndOrg(id, SecurityHelper.requireOrgId())
            .orElseThrow(() -> new IllegalArgumentException("Passenger #" + id + " not found."));
        return toDTO(e);
    }

    @Override
    public void delete(Long id) {
        TrvPassenger e = passengerRepo.findByIdAndOrg(id, SecurityHelper.requireOrgId())
            .orElseThrow(() -> new IllegalArgumentException("Passenger #" + id + " not found."));

        int tickets = ticketRepo.findByPassengerId(id).size();
        if (tickets > 0)
            throw new IllegalStateException("This passenger has " + tickets
                + " issued ticket(s). Void or remove those first.");

        Integer visas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trv_visa_applications WHERE passenger_id = ?", Integer.class, id);
        if (visas != null && visas > 0)
            throw new IllegalStateException("This passenger has " + visas
                + " visa application(s). Delete those first.");

        Integer guests = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trv_hotel_guests WHERE passenger_id = ?", Integer.class, id);
        if (guests != null && guests > 0)
            throw new IllegalStateException("This passenger is assigned to a hotel room. "
                + "Remove them from the hotel booking first.");

        preferenceRepo.deleteByPassengerId(id);
        documentService.list("PASSENGER", id)
            .forEach(d -> documentService.delete(d.getId()));
        passengerRepo.delete(e);
    }

    @Override
    public TrvPassengerDTO setLeadPassenger(Long id) {
        TrvPassenger e = passengerRepo.findByIdAndOrg(id, SecurityHelper.requireOrgId())
            .orElseThrow(() -> new IllegalArgumentException("Passenger #" + id + " not found."));
        e.setIsLeadPassenger(true);
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        passengerRepo.save(e);

        List<TrvPassenger> others = passengerRepo.findOtherLeads(e.getBooking().getId(), id);
        others.forEach(o -> o.setIsLeadPassenger(false));
        if (!others.isEmpty()) passengerRepo.saveAll(others);
        return toDTO(e);
    }

    // =========================================================================
    // LIST
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String search, Long bookingId, String expiryFilter) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        String filter = expiryFilter == null ? "" : expiryFilter.trim().toUpperCase();

        // Expiry buckets, evaluated in SQL so paging stays correct.
        String expiryClause = switch (filter) {
            case "EXPIRED"  -> " AND p.passport_expiry IS NOT NULL AND p.passport_expiry < CURRENT_DATE ";
            case "SOON"     -> " AND p.passport_expiry IS NOT NULL AND p.passport_expiry >= CURRENT_DATE "
                             + " AND p.passport_expiry < CURRENT_DATE + INTERVAL '6 months' ";
            case "MISSING"  -> " AND (p.passport_number IS NULL OR p.passport_number = '') ";
            default         -> "";
        };
        String bookingClause = bookingId != null ? " AND p.booking_id = " + bookingId + " " : "";

        return jdbcTemplate.queryForList("""
            SELECT p.id,
                   ROW_NUMBER() OVER (ORDER BY p.id DESC) AS sl,
                   COUNT(*)     OVER ()                   AS full_count,
                   b.booking_no,
                   COALESCE(p.title || ' ', '') || p.first_name
                       || COALESCE(' ' || p.last_name, '')          AS full_name,
                   COALESCE(p.passport_number, '—')                 AS passport_number,
                   COALESCE(p.nationality, '—')                     AS nationality,
                   COALESCE(TO_CHAR(p.date_of_birth, 'DD-Mon-YYYY'), '—')    AS date_of_birth,
                   COALESCE(TO_CHAR(p.passport_expiry, 'DD-Mon-YYYY'), '—')  AS passport_expiry,
                   COALESCE(p.phone, '—')                           AS phone,
                   COALESCE(p.email, '—')                           AS email,
                   CASE p.passenger_type
                       WHEN 'ADULT'  THEN '<span class="badge bg-secondary">Adult</span>'
                       WHEN 'CHILD'  THEN '<span class="badge bg-info">Child</span>'
                       WHEN 'INFANT' THEN '<span class="badge bg-warning text-dark">Infant</span>'
                       ELSE '<span class="badge bg-light text-dark">—</span>'
                   END
                   || CASE WHEN p.is_lead_passenger
                           THEN ' <span class="badge bg-primary" title="Lead passenger">Lead</span>'
                           ELSE '' END                              AS type_badge,
                   CASE
                       WHEN p.passport_number IS NULL OR p.passport_number = ''
                            THEN '<span class="badge bg-light text-dark">No passport</span>'
                       WHEN p.passport_expiry IS NULL
                            THEN '<span class="badge bg-light text-dark">No expiry</span>'
                       WHEN p.passport_expiry < CURRENT_DATE
                            THEN '<span class="badge bg-danger">Expired</span>'
                       WHEN p.passport_expiry < CURRENT_DATE + INTERVAL '6 months'
                            THEN '<span class="badge bg-warning text-dark">Under 6 months</span>'
                       ELSE '<span class="badge bg-success">Valid</span>'
                   END                                              AS passport_badge,
                   (SELECT COUNT(*) FROM trv_passenger_tickets t WHERE t.passenger_id = p.id) AS ticket_count,
                   (SELECT COUNT(*) FROM trv_documents d
                     WHERE d.entity_type = 'PASSENGER' AND d.entity_id = p.id)                AS document_count,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="pgShow('   || p.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="pgEdit('   || p.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="pgDocs('   || p.id || ')" class="btn btn-white btn-sm" title="Documents"><i class="fa fa-paperclip text-info"></i></a>'
                     || CASE WHEN p.is_lead_passenger THEN ''
                             ELSE '<a href="javascript:;" onclick="pgSetLead(' || p.id || ')" class="btn btn-white btn-sm" title="Make lead passenger"><i class="fa fa-star text-primary"></i></a>' END
                     || '<a href="javascript:;" onclick="pgDelete(' || p.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>'                                    AS actions
            FROM   trv_passengers p
            JOIN   trv_bookings   b ON b.id = p.booking_id
            WHERE  b.organization_id = ?
              AND  (p.first_name ILIKE ? OR p.last_name ILIKE ? OR p.passport_number ILIKE ?
                    OR p.phone ILIKE ? OR p.email ILIKE ? OR b.booking_no ILIKE ?)
            """ + bookingClause + expiryClause + """
            ORDER  BY p.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like, like, like, like);
    }

    // =========================================================================
    // LOOKUPS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> bookingSearch(String q) {
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT b.id,
                   b.booking_no || ' — ' || COALESCE(s.sub_account_name, 'Walk-in')
                       || ' (' || b.status || ')' AS text
            FROM   trv_bookings b
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR s.sub_account_name ILIKE ?)
            ORDER  BY b.id DESC
            LIMIT  30
            """, SecurityHelper.requireOrgId(), like, like);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> passengersForServiceLine(Long bookingServiceId) {
        return jdbcTemplate.queryForList("""
            SELECT p.id,
                   COALESCE(p.title || ' ', '') || p.first_name
                       || COALESCE(' ' || p.last_name, '')
                       || COALESCE(' — ' || p.passport_number, '') AS text
            FROM   trv_passengers p
            JOIN   trv_bookings   b  ON b.id = p.booking_id
            JOIN   trv_booking_services bs ON bs.booking_id = b.id
            WHERE  bs.id = ? AND b.organization_id = ?
            ORDER  BY p.is_lead_passenger DESC, p.id
            """, bookingServiceId, SecurityHelper.requireOrgId());
    }

    @Override
    public TrvPassengerDTO createForServiceLine(Long bookingServiceId, TrvPassengerDTO dto) {
        Long bookingId;
        try {
            bookingId = jdbcTemplate.queryForObject("""
                SELECT bs.booking_id
                FROM   trv_booking_services bs
                JOIN   trv_bookings b ON b.id = bs.booking_id
                WHERE  bs.id = ? AND b.organization_id = ?
                """, Long.class, bookingServiceId, SecurityHelper.requireOrgId());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Service line #" + bookingServiceId + " not found.");
        }
        dto.setId(null);
        dto.setBookingId(bookingId);
        return save(dto);
    }

    // =========================================================================
    // PASSPORT READING
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PassportScanDTO parseMrz(String mrzText) {
        return parseMrz(mrzText, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PassportScanDTO parseMrz(String mrzText, String vizText) {
        PassportScanDTO result = MrzParser.parse(mrzText);
        if (result.getSource() == null) result.setSource("MRZ_MANUAL");
        if (vizText != null && !vizText.isBlank()) {
            VizExtractor.apply(result, vizText);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> extractViz(String vizText, String dateOfBirth, String passportExpiry) {
        return VizExtractor.extract(vizText, safeDate(dateOfBirth), safeDate(passportExpiry));
    }

    /** Lenient ISO parse — the browser sends back what the scan gave it. */
    private static LocalDate safeDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDate.parse(iso.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PassportScanDTO scanImage(MultipartFile file) {
        if (ocrEngine.isEmpty()) {
            PassportScanDTO dto = new PassportScanDTO();
            dto.setSuccess(false);
            dto.setMessage("Server-side OCR is not enabled on this installation. "
                + "The passport is read in your browser instead — that path needs no server setup "
                + "and keeps the image on this machine.");
            return dto;
        }
        if (file == null || file.isEmpty()) {
            PassportScanDTO dto = new PassportScanDTO();
            dto.setSuccess(false);
            dto.setMessage("No image was received.");
            return dto;
        }
        try {
            String text = ocrEngine.get().extractText(file);
            PassportScanDTO dto = MrzParser.parse(text);
            // The engine returns full-page text, so the same pass also carries
            // the printed zone — harvest it for the MRZ-less fields.
            VizExtractor.apply(dto, text);
            dto.setSource("OCR_SERVER");
            return dto;
        } catch (Exception ex) {
            // Never log the OCR text itself — it is passport PII.
            log.warn("Server-side passport OCR failed: {}", ex.getClass().getSimpleName());
            PassportScanDTO dto = new PassportScanDTO();
            dto.setSuccess(false);
            dto.setMessage("The server could not read that image. Try a sharper, flatter photo, "
                + "or type the two MRZ lines manually.");
            return dto;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean serverOcrAvailable() {
        return ocrEngine.isPresent();
    }

    @Override
    public Long attachPassportImage(Long passengerId, MultipartFile file) {
        TrvPassenger e = passengerRepo.findByIdAndOrg(passengerId, SecurityHelper.requireOrgId())
            .orElseThrow(() -> new IllegalArgumentException("Passenger #" + passengerId + " not found."));

        TrvDocumentDTO doc = documentService.upload(
            "PASSENGER", passengerId, "PASSPORT", file,
            "Passport scan" + (e.getPassportNumber() != null ? " — " + e.getPassportNumber() : ""));

        e.setPassportDocumentId(doc.getId());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        passengerRepo.save(e);
        return doc.getId();
    }

    // =========================================================================
    // MAPPING + HELPERS
    // =========================================================================

    private TrvPassengerDTO toDTO(TrvPassenger e) {
        TrvPassengerDTO d = TrvPassengerDTO.builder()
            .id(e.getId())
            .bookingId(e.getBooking() != null ? e.getBooking().getId() : null)
            .bookingNo(e.getBooking() != null ? e.getBooking().getBookingNo() : null)
            .title(e.getTitle())
            .firstName(e.getFirstName())
            .lastName(e.getLastName())
            .fullName(e.getFullName())
            .dateOfBirth(e.getDateOfBirth())
            .gender(e.getGender() != null ? e.getGender().name() : null)
            .passengerType(e.getPassengerType() != null ? e.getPassengerType().name() : "ADULT")
            .isLeadPassenger(e.getIsLeadPassenger())
            .phone(e.getPhone())
            .email(e.getEmail())
            .remarks(e.getRemarks())
            .passportNumber(e.getPassportNumber())
            .passportExpiry(e.getPassportExpiry())
            .passportIssueDate(e.getPassportIssueDate())
            .nationality(e.getNationality())
            .passportCountry(e.getPassportCountry())
            .passportIssuingAuthority(e.getPassportIssuingAuthority())
            .personalNumber(e.getPersonalNumber())
            .placeOfBirth(e.getPlaceOfBirth())
            .fatherName(e.getFatherName())
            .motherName(e.getMotherName())
            .permanentAddress(e.getPermanentAddress())
            .emergencyContactName(e.getEmergencyContactName())
            .emergencyContactRelation(e.getEmergencyContactRelation())
            .emergencyContactPhone(e.getEmergencyContactPhone())
            .mrzLine1(e.getMrzLine1())
            .mrzLine2(e.getMrzLine2())
            .mrzLine3(e.getMrzLine3())
            .passportDocumentId(e.getPassportDocumentId())
            .createdBy(e.getCreatedBy())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .build();

        if (e.getDateOfBirth() != null)
            d.setAge(java.time.Period.between(e.getDateOfBirth(), LocalDate.now()).getYears());
        if (e.getPassportExpiry() != null)
            d.setDaysToExpiry(ChronoUnit.DAYS.between(LocalDate.now(), e.getPassportExpiry()));

        d.setTicketCount(ticketRepo.findByPassengerId(e.getId()).size());

        preferenceRepo.findByPassengerId(e.getId()).ifPresent(pref ->
            d.setPreference(TrvPassengerDTO.PreferenceDTO.builder()
                .id(pref.getId())
                .mealPreference(pref.getMealPreference())
                .seatPreference(pref.getSeatPreference())
                .specialAssistance(pref.getSpecialAssistance())
                .dietaryRestriction(pref.getDietaryRestriction())
                .remarks(pref.getRemarks())
                .build()));

        return d;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String trimUpper(String s) {
        String t = trim(s);
        return t == null ? null : t.toUpperCase();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
