package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvTourBookingDTO;
import com.asg.spindleserp.travel.dto.TrvTourDTO;
import com.asg.spindleserp.travel.dto.TrvTourGuideDTO;
import com.asg.spindleserp.travel.entity.TrvTour;
import com.asg.spindleserp.travel.entity.TrvTourBooking;
import com.asg.spindleserp.travel.entity.TrvTourGuide;
import com.asg.spindleserp.travel.repository.TrvTourBookingRepository;
import com.asg.spindleserp.travel.repository.TrvTourGuideRepository;
import com.asg.spindleserp.travel.repository.TrvTourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelTourServiceImpl implements TravelTourService {

    private final TrvTourRepository        tourRepo;
    private final TrvTourGuideRepository   guideRepo;
    private final TrvTourBookingRepository tourBookingRepo;
    private final JdbcTemplate             jdbcTemplate;

    // =========================================================================
    // TOURS
    // =========================================================================

    @Override
    public TrvTourDTO saveTour(TrvTourDTO dto) {
        TrvTour e = dto.getId() != null
            ? tourRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Tour #" + dto.getId() + " not found."))
            : TrvTour.builder().build();

        e.setTourCode(dto.getTourCode());
        e.setTourName(dto.getTourName());
        e.setDestination(dto.getDestination());
        e.setDurationHours(dto.getDurationHours());
        e.setBasePrice(dto.getBasePrice());
        e.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        e.setDescription(dto.getDescription());
        e.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        TrvTour saved = tourRepo.save(e);
        return TrvTourDTO.builder()
            .id(saved.getId()).tourCode(saved.getTourCode()).tourName(saved.getTourName())
            .destination(saved.getDestination()).durationHours(saved.getDurationHours())
            .basePrice(saved.getBasePrice()).currency(saved.getCurrency())
            .description(saved.getDescription()).isActive(saved.getIsActive())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TrvTourDTO findTourById(Long id) {
        TrvTour e = tourRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Tour #" + id + " not found."));
        return TrvTourDTO.builder()
            .id(e.getId()).tourCode(e.getTourCode()).tourName(e.getTourName())
            .destination(e.getDestination()).durationHours(e.getDurationHours())
            .basePrice(e.getBasePrice()).currency(e.getCurrency())
            .description(e.getDescription()).isActive(e.getIsActive())
            .build();
    }

    @Override
    public void deleteTour(Long id) { tourRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTours(String search) {
        Long orgId = SecurityHelper.requireOrgId();
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT t.id, ROW_NUMBER() OVER (ORDER BY t.id DESC) AS sl,
                   t.tour_code, t.tour_name, COALESCE(t.destination,'—') AS destination,
                   COALESCE(t.duration_hours::text,'—') AS duration_hours,
                   t.base_price, t.currency, t.is_active,
                   CASE WHEN t.is_active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-secondary">Inactive</span>' END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="tourEdit('   || t.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="tourDelete(' || t.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_tours t
            WHERE  t.organization_id = ?
              AND  (t.tour_name ILIKE ? OR t.tour_code ILIKE ? OR t.destination ILIKE ?)
            ORDER  BY t.id DESC
            """, orgId, like, like, like);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchTours(String search) {
        Long orgId = SecurityHelper.requireOrgId();
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT id, tour_code || ' — ' || tour_name AS text
            FROM   trv_tours
            WHERE  organization_id = ? AND is_active = true
              AND  (tour_name ILIKE ? OR tour_code ILIKE ?)
            ORDER  BY tour_name LIMIT 20
            """, orgId, like, like);
    }

    // =========================================================================
    // TOUR GUIDES
    // =========================================================================

    @Override
    public TrvTourGuideDTO saveGuide(TrvTourGuideDTO dto) {
        TrvTourGuide e = dto.getId() != null
            ? guideRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Guide #" + dto.getId() + " not found."))
            : new TrvTourGuide();
        e.setGuideName(dto.getGuideName());
        e.setPhone(dto.getPhone());
        e.setEmail(dto.getEmail());
        e.setLanguages(dto.getLanguages());
        e.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        TrvTourGuide saved = guideRepo.save(e);
        return TrvTourGuideDTO.builder()
            .id(saved.getId()).guideName(saved.getGuideName()).phone(saved.getPhone())
            .email(saved.getEmail()).languages(saved.getLanguages()).isActive(saved.getIsActive())
            .build();
    }

    @Override
    public void deleteGuide(Long id) { guideRepo.deleteById(id); }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listGuides() {
        return jdbcTemplate.queryForList("""
            SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sl, guide_name,
                   COALESCE(phone,'—') AS phone, COALESCE(languages,'—') AS languages, is_active,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="guideEdit('   || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="guideDelete(' || id || ')" class="btn btn-white btn-sm"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM trv_tour_guides ORDER BY guide_name
            """);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchGuides() {
        return jdbcTemplate.queryForList("""
            SELECT id, guide_name || COALESCE(' (' || languages || ')', '') AS text
            FROM trv_tour_guides WHERE is_active = true ORDER BY guide_name
            """);
    }

    // =========================================================================
    // TOUR BOOKINGS
    // =========================================================================

    @Override
    public TrvTourBookingDTO saveTourBooking(TrvTourBookingDTO dto) {
        TrvTourBooking e = dto.getId() != null
            ? tourBookingRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Tour booking #" + dto.getId() + " not found."))
            : new TrvTourBooking();

        e.setTourDate(dto.getTourDate());
        e.setPaxCount(dto.getPaxCount() != null ? dto.getPaxCount() : 1);
        e.setTotalAmount(dto.getTotalAmount());
        e.setConfirmationNumber(dto.getConfirmationNumber());
        if (e.getStatus() == null) e.setStatus(TrvTourBooking.Status.PENDING);
        e.setBookingServiceId(dto.getBookingServiceId());
        e.setTourId(dto.getTourId());
        e.setGuideId(dto.getGuideId());
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        return findTourBookingById(tourBookingRepo.save(e).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TrvTourBookingDTO findTourBookingById(Long id) {
        TrvTourBooking e = tourBookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tour booking #" + id + " not found."));
        String tourDisplay = tourRepo.findById(e.getTourId())
            .map(t -> t.getTourCode() + " — " + t.getTourName()).orElse(null);
        String guideDisplay = e.getGuideId() != null
            ? guideRepo.findById(e.getGuideId()).map(TrvTourGuide::getGuideName).orElse(null) : null;
        return TrvTourBookingDTO.builder()
            .id(e.getId()).tourDate(e.getTourDate()).paxCount(e.getPaxCount())
            .totalAmount(e.getTotalAmount()).confirmationNumber(e.getConfirmationNumber())
            .status(e.getStatus() != null ? e.getStatus().name() : null)
            .bookingServiceId(e.getBookingServiceId())
            .tourId(e.getTourId()).tourDisplay(tourDisplay)
            .guideId(e.getGuideId()).guideDisplay(guideDisplay)
            .build();
    }

    @Override
    public void deleteTourBooking(Long id) { tourBookingRepo.deleteById(id); }

    @Override
    public TrvTourBookingDTO changeTourBookingStatus(Long id, String status) {
        TrvTourBooking e = tourBookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tour booking #" + id + " not found."));
        e.setStatus(TrvTourBooking.Status.valueOf(status));
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        tourBookingRepo.save(e);
        return findTourBookingById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTourBookings(String search) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT tb.id, ROW_NUMBER() OVER (ORDER BY tb.id DESC) AS sl,
                   b.booking_no, t.tour_name, TO_CHAR(tb.tour_date,'DD-Mon-YYYY') AS tour_date,
                   tb.pax_count, COALESCE(tb.total_amount::text,'0') AS total_amount,
                   COALESCE(g.guide_name,'—') AS guide_name,
                   CASE tb.status
                       WHEN 'PENDING'   THEN '<span class="badge bg-secondary">Pending</span>'
                       WHEN 'CONFIRMED' THEN '<span class="badge bg-success">Confirmed</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'COMPLETED' THEN '<span class="badge bg-dark">Completed</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="tourbkShow('   || tb.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="tourbkEdit('   || tb.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || CASE WHEN tb.status = 'PENDING' THEN '<a href="javascript:;" onclick="tourbkConfirm(' || tb.id || ')" class="btn btn-white btn-sm" title="Confirm"><i class="fas fa-check-circle text-primary"></i></a>' ELSE '' END
                     || '<a href="javascript:;" onclick="tourbkDelete(' || tb.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_tour_bookings tb
            JOIN   trv_booking_services bs ON bs.id = tb.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            JOIN   trv_tours t ON t.id = tb.tour_id
            LEFT   JOIN trv_tour_guides g ON g.id = tb.guide_id
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR t.tour_name ILIKE ? OR tb.confirmation_number ILIKE ?)
            ORDER  BY tb.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like);
    }
}
