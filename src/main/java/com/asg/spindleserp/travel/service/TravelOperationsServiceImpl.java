package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.travel.dto.TrvAirTicketDTO;
import com.asg.spindleserp.travel.dto.TrvHotelBookingDTO;
import com.asg.spindleserp.travel.dto.TrvSupplierCostDTO;
import com.asg.spindleserp.travel.entity.*;
import com.asg.spindleserp.travel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                   hb.confirmation_number,
                   CASE hb.status
                       WHEN 'PENDING'   THEN '<span class="badge bg-secondary">Pending</span>'
                       WHEN 'CONFIRMED' THEN '<span class="badge bg-success">Confirmed</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'COMPLETED' THEN '<span class="badge bg-dark">Completed</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="hbShow('   || hb.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="hbEdit('   || hb.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || CASE WHEN hb.status = 'PENDING' THEN '<a href="javascript:;" onclick="hbConfirm(' || hb.id || ')" class="btn btn-white btn-sm" title="Confirm"><i class="fas fa-check-circle text-primary"></i></a>' ELSE '' END
                     || '<a href="javascript:;" onclick="hbDelete(' || hb.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
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
        e.setFareAmount(dto.getFareAmount());
        e.setTaxAmount(dto.getTaxAmount());
        e.setTotalAmount(dto.getTotalAmount());
        e.setSupplierReference(dto.getSupplierReference());

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
                    .status(pt.getStatus() != null ? pt.getStatus().name() : null)
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
                   COALESCE(al.airline_name,'—') AS airline_name,
                   COALESCE(o.airport_code,'—') || ' → ' || COALESCE(d.airport_code,'—') AS route,
                   TO_CHAR(COALESCE(at.departure_date, s1.departure_date),'DD-Mon-YYYY') AS departure_date,
                   COALESCE(at.total_amount::text,'0') AS total_amount,
                   (SELECT COUNT(*) FROM trv_passenger_tickets pt WHERE pt.air_ticket_id = at.id) AS pax_count,
                   (SELECT COUNT(*) FROM trv_air_ticket_segments ts WHERE ts.air_ticket_id = at.id) AS segments_count,
                   CASE at.status
                       WHEN 'ISSUED'   THEN '<span class="badge bg-success">Issued</span>'
                       WHEN 'CANCELLED' THEN '<span class="badge bg-danger">Cancelled</span>'
                       WHEN 'REFUNDED'  THEN '<span class="badge bg-secondary">Refunded</span>'
                   END AS status_badge,
                   '<div class="btn-group">'
                     || '<a href="javascript:;" onclick="atShow('   || at.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                     || '<a href="javascript:;" onclick="atEdit('   || at.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                     || '<a href="javascript:;" onclick="atDelete(' || at.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                     || '</div>' AS actions
            FROM   trv_air_tickets at
            JOIN   trv_booking_services bs ON bs.id = at.booking_service_id
            JOIN   trv_bookings b ON b.id = bs.booking_id
            LEFT   JOIN trv_airlines al ON al.id = at.airline_id
            LEFT   JOIN trv_airports o  ON o.id  = at.origin_airport_id
            LEFT   JOIN trv_airports d  ON d.id  = at.destination_airport_id
            LEFT   JOIN trv_air_ticket_segments s1 ON s1.air_ticket_id = at.id AND s1.segment_order = 1
            WHERE  b.organization_id = ?
              AND  (b.booking_no ILIKE ? OR at.pnr ILIKE ? OR al.airline_name ILIKE ?)
            ORDER  BY at.id DESC
            """, SecurityHelper.requireOrgId(), like, like, like);
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
        return jdbcTemplate.queryForList(String.format("""
            SELECT bs.id, b.booking_no || ' — ' || bs.description AS text
            FROM   trv_booking_services bs
            JOIN   trv_bookings b ON b.id = bs.booking_id
            WHERE  bs.service_type = ? AND b.organization_id = ?
              AND  bs.id NOT IN (SELECT booking_service_id FROM %s)
            ORDER  BY bs.id DESC LIMIT 30
            """, join), serviceType, SecurityHelper.requireOrgId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> passengersForServiceLine(Long bookingServiceId) {
        return jdbcTemplate.queryForList("""
            SELECT p.id, p.first_name || COALESCE(' ' || p.last_name, '') AS text
            FROM   trv_passengers p
            JOIN   trv_booking_services bs ON bs.booking_id = p.booking_id
            WHERE  bs.id = ?
            ORDER  BY p.id
            """, bookingServiceId);
    }
}
