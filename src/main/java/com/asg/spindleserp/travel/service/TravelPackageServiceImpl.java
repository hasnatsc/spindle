package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvPackageBookingDTO;
import com.asg.spindleserp.travel.dto.TrvPackageDTO;
import com.asg.spindleserp.travel.entity.*;
import com.asg.spindleserp.travel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelPackageServiceImpl implements TravelPackageService {

    private final TrvPackageRepository             packageRepo;
    private final TrvPackageItineraryDayRepository itineraryRepo;
    private final TrvPackageInclusionRepository    inclusionRepo;
    private final TrvPackageBookingRepository      packageBookingRepo;
    private final JdbcTemplate                     jdbcTemplate;

    // =========================================================================
    // PACKAGES
    // =========================================================================

    @Override
    public TrvPackageDTO savePackage(TrvPackageDTO dto) {
        TrvPackage e = dto.getId() != null
            ? packageRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Package #" + dto.getId() + " not found."))
            : TrvPackage.builder().organizationId(SecurityHelper.requireOrgId()).build();

        e.setPackageCode(dto.getPackageCode());
        e.setPackageName(dto.getPackageName());
        e.setDestination(dto.getDestination());
        e.setCategory(dto.getCategory());
        e.setDurationDays(dto.getDurationDays());
        e.setDurationNights(dto.getDurationNights());
        e.setBasePrice(dto.getBasePrice());
        e.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        e.setDescription(dto.getDescription());
        e.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        e.getItineraryDays().clear();
        if (dto.getItineraryDays() != null) {
            dto.getItineraryDays().forEach(d -> e.getItineraryDays().add(TrvPackageItineraryDay.builder()
                .dayNumber(d.getDayNumber()).title(d.getTitle()).description(d.getDescription())
                .packageEntity(e).build()));
        }
        e.getInclusions().clear();
        if (dto.getInclusions() != null) {
            dto.getInclusions().forEach(i -> e.getInclusions().add(TrvPackageInclusion.builder()
                .inclusionType(TrvPackageInclusion.InclusionType.valueOf(
                    i.getInclusionType() != null ? i.getInclusionType() : "INCLUDED"))
                .description(i.getDescription()).packageEntity(e).build()));
        }

        TrvPackage saved = packageRepo.save(e);
        return findPackageById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TrvPackageDTO findPackageById(Long id) {
        TrvPackage e = packageRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Package #" + id + " not found."));
        return TrvPackageDTO.builder()
            .id(e.getId()).packageCode(e.getPackageCode()).packageName(e.getPackageName())
            .destination(e.getDestination()).category(e.getCategory())
            .durationDays(e.getDurationDays()).durationNights(e.getDurationNights())
            .basePrice(e.getBasePrice()).currency(e.getCurrency()).description(e.getDescription())
            .isActive(e.getIsActive())
            .itineraryDays(e.getItineraryDays().stream().map(d -> TrvPackageDTO.ItineraryDayDTO.builder()
                .id(d.getId()).dayNumber(d.getDayNumber()).title(d.getTitle()).description(d.getDescription())
                .build()).collect(Collectors.toList()))
            .inclusions(e.getInclusions().stream().map(i -> TrvPackageDTO.InclusionDTO.builder()
                .id(i.getId()).inclusionType(i.getInclusionType().name()).description(i.getDescription())
                .build()).collect(Collectors.toList()))
            .build();
    }

    @Override
    public void deletePackage(Long id) {
        if (packageBookingRepo.findAll().stream().anyMatch(pb -> id.equals(pb.getPackageId())))
            throw new IllegalStateException("Cannot delete package with existing bookings against it.");
        packageRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPackages(String search) {
        Long orgId = SecurityHelper.requireOrgId();
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT p.id, ROW_NUMBER() OVER (ORDER BY p.id DESC) AS sl,
                   p.package_code, p.package_name, COALESCE(p.destination,'—') AS destination,
                   COALESCE(p.category,'—') AS category,
                   COALESCE(p.duration_days::text,'—') || 'D/' || COALESCE(p.duration_nights::text,'—') || 'N' AS duration,
                   p.base_price, p.currency, p.is_active,
                   CASE WHEN p.is_active THEN '<span class="badge bg-success">Active</span>'
                        ELSE '<span class="badge bg-secondary">Inactive</span>' END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="pkgShow('   || p.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="pkgEdit('   || p.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="pkgDelete(' || p.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_packages p
            WHERE  p.organization_id = ?
              AND  (p.package_name ILIKE ? OR p.package_code ILIKE ? OR p.destination ILIKE ?)
            ORDER  BY p.id DESC
            """, orgId, like, like, like);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchPackages(String search) {
        Long orgId = SecurityHelper.requireOrgId();
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT id, package_code || ' — ' || package_name || COALESCE(' (' || destination || ')', '') AS text
            FROM   trv_packages
            WHERE  organization_id = ? AND is_active = true
              AND  (package_name ILIKE ? OR package_code ILIKE ?)
            ORDER  BY package_name LIMIT 20
            """, orgId, like, like);
    }

    // =========================================================================
    // PACKAGE BOOKINGS
    // =========================================================================

    @Override
    public TrvPackageBookingDTO savePackageBooking(TrvPackageBookingDTO dto) {
        TrvPackageBooking e = dto.getId() != null
            ? packageBookingRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Package booking #" + dto.getId() + " not found."))
            : new TrvPackageBooking();

        e.setTravelDate(dto.getTravelDate());
        e.setPaxCount(dto.getPaxCount() != null ? dto.getPaxCount() : 1);
        e.setTotalAmount(dto.getTotalAmount());
        e.setConfirmationNumber(dto.getConfirmationNumber());
        e.setSupplierReference(dto.getSupplierReference());
        if (e.getStatus() == null) e.setStatus(TrvPackageBooking.Status.PENDING);
        e.setBookingServiceId(dto.getBookingServiceId());
        e.setPackageId(dto.getPackageId());
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        return findPackageBookingById(packageBookingRepo.save(e).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TrvPackageBookingDTO findPackageBookingById(Long id) {
        TrvPackageBooking e = packageBookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Package booking #" + id + " not found."));
        String pkgName = packageRepo.findById(e.getPackageId())
            .map(p -> p.getPackageCode() + " — " + p.getPackageName()).orElse(null);
        return TrvPackageBookingDTO.builder()
            .id(e.getId()).travelDate(e.getTravelDate()).paxCount(e.getPaxCount())
            .totalAmount(e.getTotalAmount()).confirmationNumber(e.getConfirmationNumber())
            .supplierReference(e.getSupplierReference())
            .status(e.getStatus() != null ? e.getStatus().name() : null)
            .bookingServiceId(e.getBookingServiceId()).packageId(e.getPackageId()).packageDisplay(pkgName)
            .build();
    }

    @Override
    public void deletePackageBooking(Long id) { packageBookingRepo.deleteById(id); }

    @Override
    public TrvPackageBookingDTO changePackageBookingStatus(Long id, String status) {
        TrvPackageBooking e = packageBookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Package booking #" + id + " not found."));
        e.setStatus(TrvPackageBooking.Status.valueOf(status));
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        packageBookingRepo.save(e);
        return findPackageBookingById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPackageBookings(String search) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT pb.id, ROW_NUMBER() OVER (ORDER BY pb.id DESC) AS sl,
                   b.booking_no, p.package_name, TO_CHAR(pb.travel_date,'DD-Mon-YYYY') AS travel_date,
                   pb.pax_count, COALESCE(pb.total_amount::text,'0') AS total_amount,
                   COALESCE(pb.confirmation_number,'—') AS confirmation_number,
                   CASE pb.status
                       WHEN 'PENDING'   THEN '<span class="badge bg-secondary">Pending</span>'
                       WHEN 'CONFIRMED' THEN '<span class="badge bg-success">Confirmed</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'COMPLETED' THEN '<span class="badge bg-dark">Completed</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="pkgbkShow('   || pb.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="pkgbkEdit('   || pb.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || CASE WHEN pb.status = 'PENDING' THEN '<a href="javascript:;" onclick="pkgbkConfirm(' || pb.id || ')" class="btn btn-white btn-sm" title="Confirm"><i class="fas fa-check-circle text-primary"></i></a>' ELSE '' END
                     || '<a href="javascript:;" onclick="pkgbkDelete(' || pb.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_package_bookings pb
            JOIN   trv_booking_services bs ON bs.id = pb.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            JOIN   trv_packages p ON p.id = pb.package_id
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR p.package_name ILIKE ? OR pb.confirmation_number ILIKE ?)
            ORDER  BY pb.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like);
    }
}
