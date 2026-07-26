package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.entity.JournalEntryLine;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.accounts.repository.JournalEntryMasterRepository;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import com.asg.spindleserp.travel.dto.TrvAirTicketDTO;
import com.asg.spindleserp.travel.dto.TrvHotelBookingDTO;
import com.asg.spindleserp.travel.dto.TrvSupplierCostDTO;
import com.asg.spindleserp.travel.entity.*;
import com.asg.spindleserp.travel.repository.*;
import com.asg.spindleserp.travel.entity.TrvGlAccountDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelOperationsServiceImpl implements TravelOperationsService {

    private final TrvHotelBookingRepository   hotelBookingRepo;
    private final TrvHotelRoomRepository      hotelRoomRepo;
    private final TrvHotelGuestRepository     hotelGuestRepo;
    private final TrvAirTicketRepository         airTicketRepo;
    private final TrvAirTicketSegmentRepository  airTicketSegmentRepo;
    private final TrvPassengerTicketRepository   passengerTicketRepo;
    private final TrvSupplierCostRepository      supplierCostRepo;
    private final TrvBookingServiceRepository    bookingServiceRepo;
    private final TrvGlAccountDefaultsRepository glDefaultsRepo;
    private final ChartOfAccountRepository       coaRepo;
    private final ChartOfAccountSubRepository    subRepo;
    private final JournalEntryMasterRepository   jemRepo;
    private final DocumentSequenceService        seqService;
    private final com.asg.spindleserp.organization.repository.OrganizationRepository orgRepo;
    private final TrvPassengerRepository         passengerRepo;
    private final TrvAirlineRepository           airlineRepo;
    private final TrvAirportRepository           airportRepo;
    private final TrvCabinClassRepository        cabinClassRepo;
    private final JdbcTemplate                   jdbcTemplate;

    // =========================================================================
    // HOTEL BOOKINGS
    // =========================================================================

    @Override
    public TrvHotelBookingDTO saveHotelBooking(TrvHotelBookingDTO dto) {
        TrvHotelBooking e = dto.getId() != null
            ? hotelBookingRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Hotel booking #" + dto.getId() + " not found."))
            : new TrvHotelBooking();

        e.setCheckInDate(dto.getCheckInDate());
        e.setCheckOutDate(dto.getCheckOutDate());
        e.setNights((int) ChronoUnit.DAYS.between(dto.getCheckInDate(), dto.getCheckOutDate()));
        e.setRoomsCount(dto.getRoomsCount() != null ? dto.getRoomsCount() : 1);
        e.setAdults(dto.getAdults() != null ? dto.getAdults() : 1);
        e.setChildren(dto.getChildren() != null ? dto.getChildren() : 0);
        e.setRatePerNight(dto.getRatePerNight());
        e.setTotalAmount(dto.getTotalAmount());
        e.setConfirmationNumber(dto.getConfirmationNumber());
        e.setSupplierReference(dto.getSupplierReference());

        // ── Booking source / policy fields ─────────────────────────────────────
        e.setBookingSource(dto.getBookingSource() != null
            ? TrvHotelBooking.BookingSource.valueOf(dto.getBookingSource()) : null);
        e.setCancellationPolicy(dto.getCancellationPolicy());
        e.setFreeCancellationUntil(dto.getFreeCancellationUntil());
        e.setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : BigDecimal.ZERO);
        e.setBalanceDueDate(dto.getBalanceDueDate());
        e.setSpecialRequests(dto.getSpecialRequests());
        e.setBookingCurrency(dto.getBookingCurrency() != null ? dto.getBookingCurrency() : "BDT");
        e.setTaxAmount(dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO);
        e.setNetAmount(dto.getNetAmount() != null ? dto.getNetAmount() : BigDecimal.ZERO);
        e.setVendorConfirmationReceived(dto.getVendorConfirmationReceived() != null
            ? dto.getVendorConfirmationReceived() : false);
        e.setVendorRemarks(dto.getVendorRemarks());
        if (e.getStatus() == null) e.setStatus(TrvHotelBooking.Status.PENDING);
        e.setBookingServiceId(dto.getBookingServiceId());
        e.setHotelId(dto.getHotelId());
        e.setRoomTypeId(dto.getRoomTypeId());
        e.setMealPlanId(dto.getMealPlanId());
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        TrvHotelBooking saved = hotelBookingRepo.save(e);

        // sync rooms + guests (clear/rebuild pattern)
        hotelRoomRepo.findByHotelBookingId(saved.getId()).forEach(r -> {
            hotelGuestRepo.findByHotelBookingId(saved.getId()).stream()
                .filter(g -> saved.getId().equals(g.getRoomId()) || (g.getRoomId() != null && g.getRoomId().equals(r.getId())))
                .forEach(g -> hotelGuestRepo.deleteById(g.getId()));
            hotelRoomRepo.deleteById(r.getId());
        });
        if (dto.getRooms() != null) {
            for (TrvHotelBookingDTO.RoomDTO rd : dto.getRooms()) {
                TrvHotelRoom room = hotelRoomRepo.save(TrvHotelRoom.builder()
                    .roomNumber(rd.getRoomNumber())
                    .roomTypeSnapshot(rd.getRoomTypeSnapshot())
                    .hotelBookingId(saved.getId())
                    .build());
                if (rd.getGuestPassengerIds() != null) {
                    for (Long paxId : rd.getGuestPassengerIds()) {
                        hotelGuestRepo.save(TrvHotelGuest.builder()
                            .hotelBookingId(saved.getId()).passengerId(paxId).roomId(room.getId()).build());
                    }
                }
            }
        }
        return findHotelBookingById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TrvHotelBookingDTO findHotelBookingById(Long id) {
        TrvHotelBooking e = hotelBookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Hotel booking #" + id + " not found."));
        TrvHotelBookingDTO dto = TrvHotelBookingDTO.builder()
            .id(e.getId()).checkInDate(e.getCheckInDate()).checkOutDate(e.getCheckOutDate())
            .nights(e.getNights()).roomsCount(e.getRoomsCount()).adults(e.getAdults()).children(e.getChildren())
            .ratePerNight(e.getRatePerNight()).totalAmount(e.getTotalAmount())
            .confirmationNumber(e.getConfirmationNumber()).supplierReference(e.getSupplierReference())
            .status(e.getStatus() != null ? e.getStatus().name() : null)
            .bookingServiceId(e.getBookingServiceId()).hotelId(e.getHotelId())
            .roomTypeId(e.getRoomTypeId()).mealPlanId(e.getMealPlanId())
            .bookingSource(e.getBookingSource() != null ? e.getBookingSource().name() : null)
            .cancellationPolicy(e.getCancellationPolicy())
            .freeCancellationUntil(e.getFreeCancellationUntil())
            .depositAmount(e.getDepositAmount())
            .balanceDueDate(e.getBalanceDueDate())
            .specialRequests(e.getSpecialRequests())
            .bookingCurrency(e.getBookingCurrency())
            .taxAmount(e.getTaxAmount())
            .netAmount(e.getNetAmount())
            .vendorConfirmationReceived(e.getVendorConfirmationReceived())
            .vendorRemarks(e.getVendorRemarks())
            .build();

        List<TrvHotelRoom> rooms = hotelRoomRepo.findByHotelBookingId(id);
        List<TrvHotelGuest> guests = hotelGuestRepo.findByHotelBookingId(id);
        dto.setRooms(rooms.stream().map(r -> TrvHotelBookingDTO.RoomDTO.builder()
            .id(r.getId()).roomNumber(r.getRoomNumber()).roomTypeSnapshot(r.getRoomTypeSnapshot())
            .guestPassengerIds(guests.stream()
                .filter(g -> r.getId().equals(g.getRoomId()))
                .map(TrvHotelGuest::getPassengerId).collect(Collectors.toList()))
            .build()).collect(Collectors.toList()));
        return dto;
    }

    @Override
    public void deleteHotelBooking(Long id) {
        hotelGuestRepo.findByHotelBookingId(id).forEach(g -> hotelGuestRepo.deleteById(g.getId()));
        hotelRoomRepo.findByHotelBookingId(id).forEach(r -> hotelRoomRepo.deleteById(r.getId()));
        hotelBookingRepo.deleteById(id);
    }

    @Override
    public TrvHotelBookingDTO changeHotelBookingStatus(Long id, String status) {
        TrvHotelBooking e = hotelBookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Hotel booking #" + id + " not found."));
        e.setStatus(TrvHotelBooking.Status.valueOf(status));
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        hotelBookingRepo.save(e);
        return findHotelBookingById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listHotelBookings(String search) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT hb.id, ROW_NUMBER() OVER (ORDER BY hb.id DESC) AS sl,
                   b.booking_no, TO_CHAR(hb.check_in_date,'DD-Mon-YYYY') AS check_in_date,
                   TO_CHAR(hb.check_out_date,'DD-Mon-YYYY') AS check_out_date, hb.nights,
                   h.hotel_name, COALESCE(rt.room_type_name,'—') AS room_type_name,
                   hb.rooms_count, COALESCE(hb.total_amount::text,'0') AS total_amount,
                   COALESCE(hb.deposit_amount::text,'0') AS deposit_amount,
                   COALESCE(hb.booking_source,'—') AS booking_source,
                   hb.confirmation_number,
                   CASE WHEN hb.vendor_confirmation_received THEN '<span class="badge bg-success">Yes</span>' ELSE '<span class="badge bg-warning">No</span>' END AS vendor_confirmed,
                   CASE hb.status
                       WHEN 'PENDING'   THEN '<span class="badge bg-secondary">Pending</span>'
                       WHEN 'CONFIRMED' THEN '<span class="badge bg-success">Confirmed</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'COMPLETED' THEN '<span class="badge bg-dark">Completed</span>'
                       WHEN 'NO_SHOW'   THEN '<span class="badge bg-info">No Show</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="hbShow('   || hb.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || CASE WHEN hb.status = 'PENDING' THEN '<a href="javascript:;" onclick="hbEdit('   || hb.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>' ELSE '' END
                     || CASE WHEN hb.status = 'PENDING' THEN '<a href="javascript:;" onclick="hbConfirm(' || hb.id || ')" class="btn btn-white btn-sm" title="Confirm"><i class="fas fa-check-circle text-primary"></i></a>' ELSE '' END
                     || CASE WHEN hb.status IN ('CONFIRMED','PENDING') THEN '<a href="javascript:;" onclick="hbCancel(' || hb.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-danger"></i></a>' ELSE '' END
                     || CASE WHEN hb.status = 'CONFIRMED' THEN '<a href="javascript:;" onclick="hbComplete(' || hb.id || ')" class="btn btn-white btn-sm" title="Check Out / Complete"><i class="fas fa-door-open text-dark"></i></a>' ELSE '' END
                     || CASE WHEN hb.status = 'PENDING' THEN '<a href="javascript:;" onclick="hbDelete(' || hb.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>' ELSE '' END
                     || '</div>' AS actions
            FROM   trv_hotel_bookings hb
            JOIN   trv_booking_services bs ON bs.id = hb.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            JOIN   trv_hotels h ON h.id = hb.hotel_id
            LEFT   JOIN trv_room_types rt ON rt.id = hb.room_type_id
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR h.hotel_name ILIKE ? OR hb.confirmation_number ILIKE ?)
            ORDER  BY hb.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like);
    }

    // =========================================================================
    // AIR TICKETS
    // =========================================================================

    @Override
    public TrvAirTicketDTO saveAirTicket(TrvAirTicketDTO dto) {
        TrvAirTicket e = dto.getId() != null
            ? airTicketRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Air ticket #" + dto.getId() + " not found."))
            : new TrvAirTicket();

        e.setPnr(dto.getPnr());
        e.setDepartureDate(dto.getDepartureDate());
        e.setDepartureTime(dto.getDepartureTime());
        e.setArrivalDate(dto.getArrivalDate());
        e.setArrivalTime(dto.getArrivalTime());
        e.setFareAmount(dto.getFareAmount() != null ? dto.getFareAmount() : BigDecimal.ZERO);
        e.setTaxAmount(dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO);
        e.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        e.setSupplierReference(dto.getSupplierReference());

        // ── Ticket-level fields ────────────────────────────────────────────────
        e.setTicketNumber(dto.getTicketNumber());
        e.setValidatingCarrier(dto.getValidatingCarrier());
        e.setFareBasis(dto.getFareBasis());
        e.setCommissionAmount(dto.getCommissionAmount() != null ? dto.getCommissionAmount() : BigDecimal.ZERO);
        e.setCommissionRate(dto.getCommissionRate());
        e.setNetFare(dto.getNetFare() != null ? dto.getNetFare() : BigDecimal.ZERO);
        e.setServiceFeeAmount(dto.getServiceFeeAmount() != null ? dto.getServiceFeeAmount() : BigDecimal.ZERO);
        e.setTourCode(dto.getTourCode());
        e.setEndorsementRestrictions(dto.getEndorsementRestrictions());
        e.setTicketTimeLimit(dto.getTicketTimeLimit());
        e.setAdditionalCollection(dto.getAdditionalCollection());

        // ── Vendor / Agent header fields ──────────────────────────────────────
        e.setIssueDate(dto.getIssueDate());
        e.setBookingReference(dto.getBookingReference());
        e.setAgentVendorName(dto.getAgentVendorName());
        e.setAgentVendorAddress(dto.getAgentVendorAddress());
        e.setAgentVendorEmail(dto.getAgentVendorEmail());
        e.setAgentVendorMocatNo(dto.getAgentVendorMocatNo());

        if (e.getStatus() == null) e.setStatus(TrvAirTicket.Status.ISSUED);
        e.setBookingServiceId(dto.getBookingServiceId());

        // If segments are provided, use segment[0] to populate single-segment columns (backward compat)
        List<TrvAirTicketDTO.SegmentDTO> segs = dto.getSegments();
        if (segs != null && !segs.isEmpty()) {
            TrvAirTicketDTO.SegmentDTO first = segs.get(0);
            e.setAirlineId(first.getAirlineId());
            e.setOriginAirportId(first.getOriginAirportId());
            e.setDestinationAirportId(first.getDestinationAirportId());
            e.setDepartureDate(first.getDepartureDate());
            e.setDepartureTime(first.getDepartureTime());
            e.setArrivalDate(first.getArrivalDate());
            e.setArrivalTime(first.getArrivalTime());
            e.setCabinClassId(first.getCabinClassId());
        } else {
            e.setAirlineId(dto.getAirlineId());
            e.setOriginAirportId(dto.getOriginAirportId());
            e.setDestinationAirportId(dto.getDestinationAirportId());
            e.setCabinClassId(dto.getCabinClassId());
        }
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        TrvAirTicket saved = airTicketRepo.save(e);

        // ── Sync segments (clear/rebuild) ─────────────────────────────────────
        airTicketSegmentRepo.deleteByAirTicketId(saved.getId());
        if (segs != null) {
            int order = 1;
            for (TrvAirTicketDTO.SegmentDTO sd : segs) {
                airTicketSegmentRepo.save(TrvAirTicketSegment.builder()
                    .airTicketId(saved.getId())
                    .segmentOrder(order++)
                    .flightNumber(sd.getFlightNumber())
                    .aircraftModel(sd.getAircraftModel())
                    .airlineId(sd.getAirlineId())
                    .originAirportId(sd.getOriginAirportId())
                    .destinationAirportId(sd.getDestinationAirportId())
                    .departureDate(sd.getDepartureDate())
                    .departureTime(sd.getDepartureTime())
                    .departureTerminal(sd.getDepartureTerminal())
                    .arrivalDate(sd.getArrivalDate())
                    .arrivalTime(sd.getArrivalTime())
                    .arrivalTerminal(sd.getArrivalTerminal())
                    .flightDurationMinutes(sd.getFlightDurationMinutes())
                    .cabinClassId(sd.getCabinClassId())
                    .baggageAllowance(sd.getBaggageAllowance())
                    .build());
            }
        }

        // ── Sync passenger tickets (clear/rebuild) ────────────────────────────
        passengerTicketRepo.findByAirTicketId(saved.getId())
            .forEach(pt -> passengerTicketRepo.deleteById(pt.getId()));
        if (dto.getPassengerTickets() != null) {
            for (TrvAirTicketDTO.PassengerTicketDTO ptd : dto.getPassengerTickets()) {
                if (ptd.getPassengerId() == null) continue;
                passengerTicketRepo.save(TrvPassengerTicket.builder()
                    .airTicketId(saved.getId())
                    .passengerId(ptd.getPassengerId())
                    .ticketNumber(ptd.getTicketNumber())
                    .seatNumber(ptd.getSeatNumber())
                    .baggageAllowance(ptd.getBaggageAllowance())
                    .farePortion(ptd.getFarePortion())
                    .taxPortion(ptd.getTaxPortion())
                    .status(ptd.getStatus() != null
                        ? TrvPassengerTicket.Status.valueOf(ptd.getStatus()) : TrvPassengerTicket.Status.ISSUED)
                    .build());
            }
        }
        return findAirTicketById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TrvAirTicketDTO findAirTicketById(Long id) {
        TrvAirTicket e = airTicketRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Air ticket #" + id + " not found."));
        TrvAirTicketDTO dto = TrvAirTicketDTO.builder()
            .id(e.getId()).pnr(e.getPnr())
            .departureDate(e.getDepartureDate()).departureTime(e.getDepartureTime())
            .arrivalDate(e.getArrivalDate()).arrivalTime(e.getArrivalTime())
            .fareAmount(e.getFareAmount()).taxAmount(e.getTaxAmount()).totalAmount(e.getTotalAmount())
            .supplierReference(e.getSupplierReference())
            .ticketNumber(e.getTicketNumber())
            .validatingCarrier(e.getValidatingCarrier())
            .fareBasis(e.getFareBasis())
            .commissionAmount(e.getCommissionAmount())
            .commissionRate(e.getCommissionRate())
            .netFare(e.getNetFare())
            .serviceFeeAmount(e.getServiceFeeAmount())
            .tourCode(e.getTourCode())
            .endorsementRestrictions(e.getEndorsementRestrictions())
            .ticketTimeLimit(e.getTicketTimeLimit())
            .additionalCollection(e.getAdditionalCollection())
            .issueDate(e.getIssueDate())
            .bookingReference(e.getBookingReference())
            .agentVendorName(e.getAgentVendorName())
            .agentVendorAddress(e.getAgentVendorAddress())
            .agentVendorEmail(e.getAgentVendorEmail())
            .agentVendorMocatNo(e.getAgentVendorMocatNo())
            .status(e.getStatus() != null ? e.getStatus().name() : null)
            .bookingServiceId(e.getBookingServiceId()).airlineId(e.getAirlineId())
            .originAirportId(e.getOriginAirportId()).destinationAirportId(e.getDestinationAirportId())
            .cabinClassId(e.getCabinClassId())
            .build();

        // ── Load segments ─────────────────────────────────────────────────────
        dto.setSegments(airTicketSegmentRepo.findByAirTicketIdOrderBySegmentOrderAsc(id).stream()
            .map(s -> {
                String airlineDisplay = s.getAirlineId() != null
                    ? airlineRepo.findById(s.getAirlineId()).map(a -> a.getAirlineCode() + " — " + a.getAirlineName()).orElse(null) : null;
                String originDisplay = s.getOriginAirportId() != null
                    ? airportRepo.findById(s.getOriginAirportId()).map(a -> a.getAirportCode() + " — " + a.getAirportName()).orElse(null) : null;
                String destDisplay = s.getDestinationAirportId() != null
                    ? airportRepo.findById(s.getDestinationAirportId()).map(a -> a.getAirportCode() + " — " + a.getAirportName()).orElse(null) : null;
                String cabinDisplay = s.getCabinClassId() != null
                    ? cabinClassRepo.findById(s.getCabinClassId()).map(c -> c.getClassName()).orElse(null) : null;
                return TrvAirTicketDTO.SegmentDTO.builder()
                    .id(s.getId()).segmentOrder(s.getSegmentOrder())
                    .flightNumber(s.getFlightNumber()).aircraftModel(s.getAircraftModel())
                    .airlineId(s.getAirlineId()).airlineDisplay(airlineDisplay)
                    .originAirportId(s.getOriginAirportId()).originAirportDisplay(originDisplay)
                    .destinationAirportId(s.getDestinationAirportId()).destinationAirportDisplay(destDisplay)
                    .departureDate(s.getDepartureDate()).departureTime(s.getDepartureTime())
                    .departureTerminal(s.getDepartureTerminal())
                    .arrivalDate(s.getArrivalDate()).arrivalTime(s.getArrivalTime())
                    .arrivalTerminal(s.getArrivalTerminal())
                    .flightDurationMinutes(s.getFlightDurationMinutes())
                    .cabinClassId(s.getCabinClassId()).cabinClassDisplay(cabinDisplay)
                    .baggageAllowance(s.getBaggageAllowance())
                    .build();
            }).collect(Collectors.toList()));

        // ── Load passenger tickets ────────────────────────────────────────────
        dto.setPassengerTickets(passengerTicketRepo.findByAirTicketId(id).stream()
            .map(pt -> {
                String name = passengerRepo.findById(pt.getPassengerId())
                    .map(p -> p.getFirstName() + (p.getLastName() != null ? " " + p.getLastName() : ""))
                    .orElse(null);
                return TrvAirTicketDTO.PassengerTicketDTO.builder()
                    .id(pt.getId()).passengerId(pt.getPassengerId()).passengerName(name)
                    .ticketNumber(pt.getTicketNumber()).seatNumber(pt.getSeatNumber())
                    .baggageAllowance(pt.getBaggageAllowance())
                    .farePortion(pt.getFarePortion())
                    .taxPortion(pt.getTaxPortion())
                    .status(pt.getStatus() != null ? pt.getStatus().name() : null)
                    .checkInStatus(pt.getCheckInStatus() != null ? pt.getCheckInStatus().name() : null)
                    .checkInTime(pt.getCheckInTime())
                    .boardingTime(pt.getBoardingTime())
                    .gateNumber(pt.getGateNumber())
                    .departureGate(pt.getDepartureGate())
                    .build();
            }).collect(Collectors.toList()));
        return dto;
    }

    @Override
    public void deleteAirTicket(Long id) {
        airTicketSegmentRepo.deleteByAirTicketId(id);
        passengerTicketRepo.findByAirTicketId(id).forEach(pt -> passengerTicketRepo.deleteById(pt.getId()));
        airTicketRepo.deleteById(id);
    }

    @Override
    public TrvAirTicketDTO changeAirTicketStatus(Long id, String status) {
        TrvAirTicket e = airTicketRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Air ticket #" + id + " not found."));
        e.setStatus(TrvAirTicket.Status.valueOf(status));
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        airTicketRepo.save(e);
        return findAirTicketById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAirTickets(String search) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT at.id, ROW_NUMBER() OVER (ORDER BY at.id DESC) AS sl,
                   b.booking_no, COALESCE(at.pnr,'—') AS pnr,
                   COALESCE(at.ticket_number,'—') AS ticket_number,
                   COALESCE(al.airline_name,'—') AS airline_name,
                   COALESCE(o.airport_code,'—') || ' → ' || COALESCE(d.airport_code,'—') AS route,
                   TO_CHAR(COALESCE(at.departure_date, s1.departure_date),'DD-Mon-YYYY') AS departure_date,
                   COALESCE(at.fare_amount::text,'0') AS fare_amount,
                   COALESCE(at.tax_amount::text,'0') AS tax_amount,
                   COALESCE(at.total_amount::text,'0') AS total_amount,
                   (SELECT COUNT(*) FROM trv_passenger_tickets pt WHERE pt.air_ticket_id = at.id) AS pax_count,
                   (SELECT COUNT(*) FROM trv_passenger_tickets pt WHERE pt.air_ticket_id = at.id AND pt.status = 'CHECKED_IN') AS checked_in_count,
                   (SELECT COUNT(*) FROM trv_air_ticket_segments ts WHERE ts.air_ticket_id = at.id) AS segments_count,
                   CASE at.status
                       WHEN 'ISSUED'   THEN '<span class="badge bg-success">Issued</span>'
                       WHEN 'VOID'     THEN '<span class="badge bg-secondary">Void</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'REFUNDED'  THEN '<span class="badge bg-info">Refunded</span>'
                       WHEN 'EXCHANGED' THEN '<span class="badge bg-primary">Exchanged</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="atShow('   || at.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || CASE WHEN at.status = 'ISSUED' THEN '<a href="javascript:;" onclick="atEdit('   || at.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>' ELSE '' END
                     || CASE WHEN at.status = 'ISSUED' THEN '<a href="javascript:;" onclick="atVoid('   || at.id || ')" class="btn btn-white btn-sm" title="Void"><i class="fas fa-ban text-secondary"></i></a>' ELSE '' END
                     || CASE WHEN at.status = 'ISSUED' THEN '<a href="javascript:;" onclick="atCancel(' || at.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fa-regular fa-trash-can text-danger"></i></a>' ELSE '' END
                     || CASE WHEN at.status IN ('ISSUED','CANCELLED') THEN '<a href="javascript:;" onclick="atRefund(' || at.id || ')" class="btn btn-white btn-sm" title="Refund"><i class="fas fa-undo text-info"></i></a>' ELSE '' END
                     || '</div>' AS actions
            FROM   trv_air_tickets at
            JOIN   trv_booking_services bs ON bs.id = at.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            LEFT   JOIN trv_airlines al ON al.id = at.airline_id
            LEFT   JOIN trv_airports o  ON o.id  = at.origin_airport_id
            LEFT   JOIN trv_airports d  ON d.id  = at.destination_airport_id
            LEFT   JOIN trv_air_ticket_segments s1 ON s1.air_ticket_id = at.id AND s1.segment_order = 1
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR at.pnr ILIKE ? OR al.airline_name ILIKE ? OR at.ticket_number ILIKE ?)
            ORDER  BY at.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like, like);
    }

    // =========================================================================
    // SUPPLIER COSTS
    // =========================================================================

    @Override
    public TrvSupplierCostDTO saveSupplierCost(TrvSupplierCostDTO dto) {
        TrvSupplierCost e = dto.getId() != null
            ? supplierCostRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Supplier cost #" + dto.getId() + " not found."))
            : new TrvSupplierCost();

        e.setCostAmount(dto.getCostAmount());
        e.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        e.setPaymentStatus(dto.getPaymentStatus() != null
            ? TrvSupplierCost.PaymentStatus.valueOf(dto.getPaymentStatus()) : TrvSupplierCost.PaymentStatus.UNPAID);
        e.setInvoiceReference(dto.getInvoiceReference());
        e.setBookingServiceId(dto.getBookingServiceId());
        e.setSupplierId(dto.getSupplierId());
        e.setCreatedBy(e.getCreatedBy() == null ? SecurityHelper.currentUsername().orElse("system") : e.getCreatedBy());
        e.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));

        TrvSupplierCost saved = supplierCostRepo.save(e);
        return TrvSupplierCostDTO.builder()
            .id(saved.getId()).costAmount(saved.getCostAmount()).currency(saved.getCurrency())
            .paymentStatus(saved.getPaymentStatus().name()).invoiceReference(saved.getInvoiceReference())
            .bookingServiceId(saved.getBookingServiceId()).supplierId(saved.getSupplierId())
            .build();
    }

    @Override
    public void deleteSupplierCost(Long id) { supplierCostRepo.deleteById(id); }

    @Override
    public TrvSupplierCostDTO postSupplierCostToGl(Long id) {
        TrvSupplierCost cost = supplierCostRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier cost #" + id + " not found."));
        if (cost.getJournalEntryId() != null)
            throw new IllegalStateException("Supplier cost #" + id + " already posted to GL.");

        Long orgId = ContextProvider.getOrganizationId();
        String user = SecurityHelper.currentUsername().orElse("system");
        String year = String.valueOf(java.time.LocalDate.now().getYear()).substring(2);

        // Resolve GL accounts from org defaults
        TrvGlAccountDefaults glDefaults = glDefaultsRepo.findByOrganizationId(orgId).orElse(null);
        ChartOfAccount costOfService = null;
        if (glDefaults != null && glDefaults.getCostOfServiceAccountId() != null)
            costOfService = coaRepo.findById(glDefaults.getCostOfServiceAccountId()).orElse(null);

        // CR: supplier sub-account main account, or supplier payable default
        ChartOfAccount crAccount = null;
        ChartOfAccountSub supplierSub = null;
        if (cost.getSupplierId() != null) {
            supplierSub = subRepo.findById(cost.getSupplierId()).orElse(null);
            if (supplierSub != null) crAccount = supplierSub.getMainAccount();
        }
        if (crAccount == null && glDefaults != null && glDefaults.getSupplierPayableDefaultId() != null)
            crAccount = coaRepo.findById(glDefaults.getSupplierPayableDefaultId()).orElse(null);

        // Build JEM
        String voucherNo = seqService.nextDocumentNumber(orgId, "DN", year);
        JournalEntryMaster jem = new JournalEntryMaster();
        jem.setOrganization(orgRepo.getReferenceById(orgId));
        jem.setVoucherType(VoucherType.DEBIT_NOTE);
        jem.setVoucherNo(voucherNo);
        jem.setVoucherDate(java.time.LocalDate.now());
        jem.setVoucherStatus("POSTED");
        jem.setPosted(true);
        jem.setPostedBy(user);
        jem.setPostedAt(java.time.LocalDateTime.now());
        jem.setReversed(false);
        jem.setTotalAmount(cost.getCostAmount());
        jem.setTotalDebit(cost.getCostAmount());
        jem.setTotalCredit(cost.getCostAmount());
        jem.setAllocatedAmount(java.math.BigDecimal.ZERO);
        jem.setPartyId(cost.getSupplierId());
        jem.setPartyType("SUPPLIER");
        jem.setReferenceNo(cost.getInvoiceReference());
        jem.setNarration("Supplier cost: " + (cost.getInvoiceReference() != null ? cost.getInvoiceReference() : "SC#" + id));
        jem.setCreatedBy(user);
        jem.setUpdatedBy(user);

        // DR: Cost of Service
        int lineNo = 1;
        JournalEntryLine drLine = new JournalEntryLine();
        drLine.setJournalEntry(jem);
        drLine.setLineNumber(lineNo++);
        drLine.setAccount(costOfService);
        drLine.setEntryType(JournalEntryLine.EntryType.DEBIT);
        drLine.setAmount(cost.getCostAmount());
        drLine.setNarration("Cost of service — " + (cost.getInvoiceReference() != null ? cost.getInvoiceReference() : "SC#" + id));
        drLine.setOrganization(orgRepo.getReferenceById(orgId));
        drLine.setTaxLine(false);
        jem.getLines().add(drLine);

        // CR: Supplier Payable
        JournalEntryLine crLine = new JournalEntryLine();
        crLine.setJournalEntry(jem);
        crLine.setLineNumber(lineNo);
        crLine.setAccount(crAccount);
        crLine.setSubAccount(supplierSub);
        crLine.setEntryType(JournalEntryLine.EntryType.CREDIT);
        crLine.setAmount(cost.getCostAmount());
        crLine.setNarration("Payable to supplier — " + (cost.getInvoiceReference() != null ? cost.getInvoiceReference() : "SC#" + id));
        crLine.setOrganization(orgRepo.getReferenceById(orgId));
        crLine.setTaxLine(false);
        jem.getLines().add(crLine);

        JournalEntryMaster saved = jemRepo.save(jem);

        // Update supplier sub-account balance (increase — we owe them)
        if (supplierSub != null) {
            java.math.BigDecimal bal = supplierSub.getCurrentBalance() != null
                ? supplierSub.getCurrentBalance() : java.math.BigDecimal.ZERO;
            supplierSub.setCurrentBalance(bal.add(cost.getCostAmount()));
            subRepo.save(supplierSub);
        }

        // Mark supplier cost as posted
        cost.setJournalEntryId(saved.getId());
        cost.setPaymentStatus(TrvSupplierCost.PaymentStatus.UNPAID);
        supplierCostRepo.save(cost);

        return toDto(cost);
    }

    private TrvSupplierCostDTO toDto(TrvSupplierCost cost) {
        return TrvSupplierCostDTO.builder()
            .id(cost.getId())
            .costAmount(cost.getCostAmount())
            .currency(cost.getCurrency())
            .paymentStatus(cost.getPaymentStatus().name())
            .invoiceReference(cost.getInvoiceReference())
            .bookingServiceId(cost.getBookingServiceId())
            .supplierId(cost.getSupplierId())
            .journalEntryId(cost.getJournalEntryId())
            .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSupplierCosts(String search) {
        String like = "%" + (search == null ? "" : search.trim()) + "%";
        return jdbcTemplate.queryForList("""
            SELECT sc.id, ROW_NUMBER() OVER (ORDER BY sc.id DESC) AS sl,
                   b.booking_no, COALESCE(s.sub_account_name,'—') AS supplier_name,
                   COALESCE(sc.cost_amount::text,'0') AS cost_amount, sc.currency,
                   COALESCE(sc.invoice_reference,'—') AS invoice_reference,
                   CASE sc.payment_status
                       WHEN 'UNPAID'  THEN '<span class="badge bg-danger">Unpaid</span>'
                       WHEN 'PARTIAL' THEN '<span class="badge bg-warning">Partial</span>'
                       WHEN 'PAID'    THEN '<span class="badge bg-success">Paid</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="scEdit('   || sc.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="scDelete(' || sc.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_supplier_costs sc
            JOIN   trv_booking_services bs ON bs.id = sc.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = sc.supplier_id
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR s.sub_account_name ILIKE ? OR sc.invoice_reference ILIKE ?)
            ORDER  BY sc.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like);
    }

    // =========================================================================
    // LOOKUPS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> unfulfilledServiceLines(String serviceType) {
        String join = switch (serviceType) {
            case "HOTEL" -> "trv_hotel_bookings";
            case "AIR" -> "trv_air_tickets";
            case "PACKAGE" -> "trv_package_bookings";
            case "TOUR" -> "trv_tour_bookings";
            case "VISA" -> "trv_visa_applications";
            default -> throw new IllegalArgumentException("Unknown service type: " + serviceType);
        };
        String selectCols = "AIR".equals(serviceType)
            ? "bs.id, b.booking_no, bs.pnr, bs.description, b.booking_no || ' — ' || bs.description AS text,"
              + " (SELECT COUNT(*) FROM trv_passengers p WHERE p.booking_id = b.id) AS pax_count"
            : "bs.id, b.booking_no || ' — ' || bs.description AS text";
        return jdbcTemplate.queryForList(String.format("""
            SELECT %s
            FROM   trv_booking_services bs
            JOIN   trv_bookings b ON b.id = bs.booking_id
            WHERE  bs.service_type = ? AND b.organization_id = ?
              AND  bs.id NOT IN (SELECT booking_service_id FROM %s)
            ORDER  BY bs.id DESC LIMIT 30
            """, selectCols, join), serviceType, SecurityHelper.requireOrgId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> passengersForServiceLine(Long bookingServiceId) {
        return jdbcTemplate.queryForList("""
            SELECT p.id, p.first_name || COALESCE(' ' || p.last_name, '') AS text,
                   p.date_of_birth, p.passport_expiry, p.passport_number, p.nationality
            FROM   trv_passengers p
            JOIN   trv_booking_services bs ON bs.booking_id = p.booking_id
            WHERE  bs.id = ?
            ORDER  BY p.id
            """, bookingServiceId);
    }

    @Override
    public Map<String, Object> createPassenger(Long bookingServiceId, Map<String, Object> data) {
        TrvBookingService svc = bookingServiceRepo.findById(bookingServiceId)
            .orElseThrow(() -> new IllegalArgumentException("Service line #" + bookingServiceId + " not found."));
        TrvPassenger p = TrvPassenger.builder()
            .title((String) data.get("title"))
            .firstName((String) data.getOrDefault("firstName", ""))
            .lastName((String) data.get("lastName"))
            .dateOfBirth(data.get("dateOfBirth") != null ? LocalDate.parse((String) data.get("dateOfBirth")) : null)
            .gender(data.get("gender") != null ? TrvPassenger.Gender.valueOf((String) data.get("gender")) : null)
            .passportNumber((String) data.get("passportNumber"))
            .passportExpiry(data.get("passportExpiry") != null ? LocalDate.parse((String) data.get("passportExpiry")) : null)
            .nationality((String) data.get("nationality"))
            .passengerType(data.get("passengerType") != null
                ? TrvPassenger.PassengerType.valueOf((String) data.get("passengerType")) : TrvPassenger.PassengerType.ADULT)
            .phone((String) data.get("phone"))
            .email((String) data.get("email"))
            .booking(svc.getBooking())
            .build();
        TrvPassenger saved = passengerRepo.save(p);
        return Map.of(
            "id", saved.getId(),
            "text", saved.getFirstName() + (saved.getLastName() != null ? " " + saved.getLastName() : ""),
            "date_of_birth", saved.getDateOfBirth() != null ? saved.getDateOfBirth().toString() : null,
            "passport_expiry", saved.getPassportExpiry() != null ? saved.getPassportExpiry().toString() : null,
            "passport_number", saved.getPassportNumber(),
            "nationality", saved.getNationality()
        );
    }
}
