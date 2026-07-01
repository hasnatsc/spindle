package com.asg.spindleserp.travel.service;

import com.asg.spindleserp.accounts.dto.VoucherDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.entity.JournalEntryLine;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.accounts.repository.ChartOfAccountRepository;
import com.asg.spindleserp.accounts.repository.ChartOfAccountSubRepository;
import com.asg.spindleserp.accounts.repository.JournalEntryMasterRepository;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.setup.service.DocumentSequenceService;
import com.asg.spindleserp.travel.dto.TrvBookingDTO;
import com.asg.spindleserp.travel.entity.*;
import com.asg.spindleserp.travel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TravelBookingServiceImpl
 *
 * Mirrors SalesServiceImpl.confirmInvoice() for the GL bridge: a confirmed
 * booking creates a SALES_VOUCHER JournalEntryMaster (DR Accounts Receivable
 * / CR Travel Revenue), referenceNo = booking_no, so it can be settled
 * through the existing Receipt Voucher allocation flow — no new voucher
 * type or new allocation logic required.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TravelBookingServiceImpl implements TravelBookingService {

    private final TrvBookingRepository               bookingRepo;
    private final TrvBookingServiceRepository        serviceLineRepo;
    private final TrvPassengerRepository             passengerRepo;
    private final TrvPassengerPreferenceRepository   preferenceRepo;
    private final TrvBookingStatusHistoryRepository  historyRepo;
    private final TrvGlAccountDefaultsRepository     glDefaultsRepo;

    private final ChartOfAccountRepository           coaRepo;
    private final ChartOfAccountSubRepository        subRepo;
    private final JournalEntryMasterRepository       jemRepo;
    private final OrganizationRepository             orgRepo;
    private final DocumentSequenceService            seqService;
    private final JdbcTemplate                       jdbcTemplate;

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public TrvBookingDTO save(TrvBookingDTO dto) {
        TrvBooking entity;
        Long orgId = SecurityHelper.requireOrgId();

        if (dto.getId() != null) {
            entity = findBooking(dto.getId());
            guardDraft(entity);
        } else {
            entity = TrvBooking.builder()
                .status(TrvBooking.Status.DRAFT)
                .build();
        }

        buildHeader(dto, entity);
        syncServices(dto, entity);
        syncPassengers(dto, entity);
        recalcTotals(entity);

        boolean isCreate = entity.getId() == null;
        TrvBooking saved = bookingRepo.save(entity);
        syncPreferencesAfterSave(dto, saved);
        if (isCreate) logStatus(saved, "DRAFT", "Booking created.");
        return toDTO(saved);
    }

    // =========================================================================
    // CONFIRM  (creates SALES_VOUCHER JEM, mirrors SalesServiceImpl.confirmInvoice)
    // =========================================================================

    @Override
    public TrvBookingDTO confirm(Long id) {
        TrvBooking booking = findBooking(id);
        guardDraft(booking);

        if (booking.getPartyId() == null)
            throw new IllegalStateException("Customer (party) is required to confirm a booking.");
        if (booking.getServices().isEmpty())
            throw new IllegalStateException("At least one service line is required to confirm a booking.");
        if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalStateException("Booking total amount cannot be zero.");

        Long orgId  = ContextProvider.getOrganizationId();
        String user = SecurityHelper.currentUsername().orElse("system");
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);

        // ── Step 1: Resolve AR account from customer's main account ──────────
        ChartOfAccountSub customerSub = subRepo.findById(booking.getPartyId())
            .orElseThrow(() -> new IllegalStateException("Customer account not found: " + booking.getPartyId()));
        ChartOfAccount arAccount = customerSub.getMainAccount();
        if (arAccount == null)
            throw new IllegalStateException(
                "Customer '" + customerSub.getSubAccountName() +
                "' has no linked main account (AR control account). Set it on the customer sub-account first.");

        // ── Step 2: Resolve Travel Revenue account (org defaults first) ──────
        ChartOfAccount revenueAccount = resolveRevenueAccount(orgId);
        if (revenueAccount == null)
            throw new IllegalStateException(
                "No Travel Revenue account configured. Set it in Travel → Settings, or create a " +
                "REVENUE account with code 'TRAVEL-REVENUE' in this organisation.");

        // ── Step 3: Build SALES_VOUCHER JEM (same type Sales/eCommerce use) ──
        String voucherNo = seqService.nextDocumentNumber(orgId, "TRV", year);

        JournalEntryMaster jem = new JournalEntryMaster();
        jem.setOrganization(orgRepo.getReferenceById(orgId));
        jem.setVoucherType(VoucherType.SALES_VOUCHER);
        jem.setVoucherNo(voucherNo);
        jem.setVoucherDate(booking.getBookingDate());
        jem.setDueDate(booking.getTravelStartDate() != null
            ? booking.getTravelStartDate() : booking.getBookingDate().plusDays(15));
        jem.setVoucherStatus("POSTED");
        jem.setPosted(true);
        jem.setReversed(false);
        jem.setTotalAmount(booking.getTotalAmount());
        jem.setTotalDebit(booking.getTotalAmount());
        jem.setTotalCredit(booking.getTotalAmount());
        jem.setAllocatedAmount(BigDecimal.ZERO);
        jem.setPartyId(booking.getPartyId());
        jem.setPartyType("CUSTOMER");
        jem.setReferenceNo(booking.getBookingNo());
        jem.setNarration("Travel Booking: " + booking.getBookingNo() + " (" + booking.getBookingType() + ")");
        jem.setPostedBy(user);
        jem.setPostedAt(LocalDateTime.now());
        jem.setCreatedBy(user);
        jem.setUpdatedBy(user);

        JournalEntryLine drLine = new JournalEntryLine();
        drLine.setJournalEntry(jem);
        drLine.setLineNumber(1);
        drLine.setAccount(arAccount);
        drLine.setSubAccount(customerSub);
        drLine.setEntryType(JournalEntryLine.EntryType.DEBIT);
        drLine.setAmount(booking.getTotalAmount());
        drLine.setNarration("AR: " + customerSub.getSubAccountCode() + " — " + customerSub.getSubAccountName());
        drLine.setOrganization(orgRepo.getReferenceById(orgId));
        drLine.setTaxLine(false);

        JournalEntryLine crLine = new JournalEntryLine();
        crLine.setJournalEntry(jem);
        crLine.setLineNumber(2);
        crLine.setAccount(revenueAccount);
        crLine.setEntryType(JournalEntryLine.EntryType.CREDIT);
        crLine.setAmount(booking.getTotalAmount());
        crLine.setNarration("Travel Revenue: " + booking.getBookingNo());
        crLine.setOrganization(orgRepo.getReferenceById(orgId));
        crLine.setTaxLine(false);

        jem.getLines().add(drLine);
        jem.getLines().add(crLine);

        JournalEntryMaster savedJem = jemRepo.save(jem);
        log.info("Booking {} confirmed. SALES_VOUCHER {} created. Customer: {}",
                 booking.getBookingNo(), savedJem.getVoucherNo(), customerSub.getSubAccountName());

        // ── Step 4: Update customer AR balance ────────────────────────────────
        BigDecimal current = customerSub.getCurrentBalance() != null ? customerSub.getCurrentBalance() : BigDecimal.ZERO;
        customerSub.setCurrentBalance(current.add(booking.getTotalAmount()));
        subRepo.save(customerSub);

        // ── Step 5: Update booking ─────────────────────────────────────────────
        booking.setJournalEntryId(savedJem.getId());
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDueAmount(booking.getTotalAmount());
        booking.setStatus(TrvBooking.Status.CONFIRMED);
        booking.setUpdatedBy(user);
        booking.setUpdatedAt(LocalDateTime.now());

        TrvBooking saved = bookingRepo.save(booking);
        logStatus(saved, "CONFIRMED", "Confirmed. Voucher " + savedJem.getVoucherNo() + " posted.");
        return toDTO(saved);
    }

    /**
     * Resolves the Travel Revenue account for this org.
     * Priority: 1) trv_gl_account_defaults.travelRevenueAccountId (explicit config)
     *           2) first active REVENUE account with code starting 'TRAVEL'
     *           3) any first active REVENUE account
     */
    private ChartOfAccount resolveRevenueAccount(Long orgId) {
        Optional<TrvGlAccountDefaults> defaults = glDefaultsRepo.findByOrganizationId(orgId);
        if (defaults.isPresent() && defaults.get().getTravelRevenueAccountId() != null) {
            return coaRepo.findById(defaults.get().getTravelRevenueAccountId()).orElse(null);
        }
        Optional<ChartOfAccount> byPrefix = coaRepo
            .findFirstByOrganizationIdAndAccountTypeAndAccountCodeStartingWithIgnoreCaseAndIsActiveTrue(
                orgId, ChartOfAccount.AccountType.REVENUE, "TRAVEL");
        if (byPrefix.isPresent()) return byPrefix.get();

        return coaRepo
            .findFirstByOrganizationIdAndAccountTypeAndIsActiveTrue(orgId, ChartOfAccount.AccountType.REVENUE)
            .orElse(null);
    }

    // =========================================================================
    // CANCEL / DELETE
    // =========================================================================

    @Override
    public TrvBookingDTO cancel(Long id) {
        TrvBooking booking = findBooking(id);
        guardDraft(booking); // confirmed bookings need a reversal flow — not in Phase 1 scope
        booking.setStatus(TrvBooking.Status.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setUpdatedBy(SecurityHelper.currentUsername().orElse("system"));
        TrvBooking saved = bookingRepo.save(booking);
        logStatus(saved, "CANCELLED", "Booking cancelled.");
        return toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        TrvBooking booking = findBooking(id);
        if (booking.getStatus() == TrvBooking.Status.CONFIRMED)
            throw new IllegalStateException("Confirmed bookings cannot be deleted. Cancel first.");
        bookingRepo.delete(booking); // cascades to services + passengers (+ preferences via FK cascade)
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public TrvBookingDTO findById(Long id) {
        return toDTO(findBooking(id));
    }

    // =========================================================================
    // RECEIPT PREFILL  (mirror of SalesServiceImpl.populateReceiptFromInvoice)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public VoucherDTO populateReceiptFromBooking(Long bookingId) {
        TrvBooking booking = findBooking(bookingId);

        if (booking.getStatus() != TrvBooking.Status.CONFIRMED
                && booking.getStatus() != TrvBooking.Status.PARTIALLY_PAID)
            throw new IllegalStateException(
                "Booking must be CONFIRMED before creating a receipt. Status: " + booking.getStatus());

        BigDecimal dueAmount = booking.getDueAmount() != null ? booking.getDueAmount() : BigDecimal.ZERO;
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("Booking " + booking.getBookingNo() + " is already fully collected.");

        JournalEntryMaster jem = jemRepo
            .findByOrganizationIdAndReferenceNoAndVoucherType(
                ContextProvider.getOrganizationId(), booking.getBookingNo(), VoucherType.SALES_VOUCHER)
            .orElseThrow(() -> new IllegalStateException(
                "No accounting voucher found for booking " + booking.getBookingNo() +
                ". Please confirm the booking again to regenerate the accounting entry."));

        ChartOfAccountSub customerSub = subRepo.findById(booking.getPartyId()).orElse(null);

        VoucherDTO dto = new VoucherDTO();
        dto.setVoucherType("RECEIPT_VOUCHER");
        dto.setVoucherStatus("DRAFT");
        dto.setVoucherDate(LocalDate.now());
        dto.setDueDate(booking.getTravelStartDate());
        dto.setTotalAmount(dueAmount);
        dto.setPartyType("CUSTOMER");
        dto.setPartyId(booking.getPartyId());
        dto.setPartyDisplay(customerSub != null
            ? customerSub.getSubAccountCode() + " — " + customerSub.getSubAccountName() : null);
        dto.setPartyBalance(customerSub != null ? customerSub.getCurrentBalance() : null);
        dto.setReferenceNo(booking.getBookingNo());
        dto.setNarration("Receipt against Travel Booking: " + booking.getBookingNo());

        VoucherDTO.AllocationDTO alloc = new VoucherDTO.AllocationDTO();
        alloc.setSourceVoucherId(jem.getId());
        alloc.setSourceVoucherNo(jem.getVoucherNo());
        alloc.setSourceVoucherType(jem.getVoucherType().name());
        alloc.setSourceDueDate(jem.getDueDate());
        alloc.setSourceTotal(jem.getTotalAmount());
        alloc.setSourceAlreadyAllocated(jem.getAllocatedAmount());
        alloc.setSourceRemaining(dueAmount);
        alloc.setAllocatedAmount(dueAmount);
        alloc.setDiscountAmount(BigDecimal.ZERO);
        alloc.setWriteOffAmount(BigDecimal.ZERO);
        alloc.setAllocationDate(LocalDate.now());
        alloc.setNarration("Settlement of " + booking.getBookingNo());
        dto.setAllocations(List.of(alloc));

        return dto;
    }

    // =========================================================================
    // DATATABLE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search, String status) {
        Long orgId = SecurityHelper.requireOrgId();

        String where = "WHERE b.organization_id = " + orgId
            + (status != null && !status.isBlank() ? " AND b.status = '" + status + "'" : "")
            + CommonUtils.searchILike(search, Arrays.asList(
                "b.booking_no", "s.sub_account_code", "s.sub_account_name", "b.status"));

        String sql = String.format("""
            SELECT
                ROW_NUMBER() OVER (ORDER BY b.id DESC)                            AS sl,
                COUNT(*)     OVER ()                                              AS full_count,
                b.id,
                b.booking_no,
                b.booking_type,
                TO_CHAR(b.booking_date, 'DD-Mon-YYYY')                            AS booking_date,
                TO_CHAR(b.travel_start_date, 'DD-Mon-YYYY')                       AS travel_start_date,
                COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—') AS customer_name,
                COALESCE(u.full_name, u.username, '—')                           AS sales_agent,
                COALESCE(b.total_amount::text, '0')                              AS total_amount,
                COALESCE(b.paid_amount::text,  '0')                              AS paid_amount,
                COALESCE(b.due_amount::text,   '0')                              AS due_amount,
                (SELECT COUNT(*) FROM trv_passengers p WHERE p.booking_id = b.id) AS passenger_count,
                CASE b.status
                    WHEN 'DRAFT'           THEN '<span class="badge bg-secondary">Draft</span>'
                    WHEN 'CONFIRMED'       THEN '<span class="badge bg-success">Confirmed</span>'
                    WHEN 'PARTIALLY_PAID'  THEN '<span class="badge bg-warning">Partially Paid</span>'
                    WHEN 'PAID'            THEN '<span class="badge bg-primary">Paid</span>'
                    WHEN 'CANCELLED'       THEN '<span class="badge bg-danger">Cancelled</span>'
                    WHEN 'COMPLETED'       THEN '<span class="badge bg-dark">Completed</span>'
                    ELSE '<span class="badge bg-info">' || b.status || '</span>'
                END AS status_badge,
                '<div class="btn-group">'
                    || '<a href="javascript:;" onclick="bkgShow('    || b.id || ')" class="btn btn-white btn-sm" title="View"><i class="fas fa-eye text-success"></i></a>'
                    || CASE WHEN b.status = 'DRAFT' THEN
                        '<a href="javascript:;" onclick="bkgEdit('    || b.id || ')" class="btn btn-white btn-sm" title="Edit"><i class="fa-regular fa-pen-to-square text-warning"></i></a>'
                        || '<a href="javascript:;" onclick="bkgConfirm(' || b.id || ')" class="btn btn-white btn-sm" title="Confirm"><i class="fas fa-check-circle text-primary"></i></a>'
                        || '<a href="javascript:;" onclick="bkgCancel('  || b.id || ')" class="btn btn-white btn-sm" title="Cancel"><i class="fas fa-ban text-secondary"></i></a>'
                        || '<a href="javascript:;" onclick="bkgDelete('  || b.id || ')" class="btn btn-white btn-sm" title="Delete"><i class="fa-regular fa-trash-can text-danger"></i></a>'
                       ELSE '' END
                    || CASE WHEN b.status IN ('CONFIRMED','PARTIALLY_PAID') AND COALESCE(b.due_amount,0) > 0 THEN
                        '<a href="javascript:;" onclick="createReceiptFromBooking(' || b.id || ')" class="btn btn-white btn-sm" title="Collect Payment"><i class="fas fa-money-bill-wave text-primary"></i></a>'
                       ELSE '' END
                    || '</div>'                                                   AS actions
            FROM   trv_bookings b
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            LEFT   JOIN sec_users u ON u.id = b.sales_agent_id
            %s
            ORDER  BY b.id DESC
            OFFSET %d LIMIT %d
            """, where, start, length);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.get(0).get("full_count"));
        return DataTableResponse.of(draw, total, total, rows);
    }

    // =========================================================================
    // DASHBOARD SUMMARY
    // =========================================================================
    //
    // Verified against the live schema (organization_id is present directly
    // on trv_hotel_bookings, trv_air_tickets, trv_supplier_costs,
    // trv_visa_applications, trv_tour_bookings, trv_package_bookings,
    // trv_passengers, trv_room_types, trv_hotel_categories — inherited from
    // BaseEntity and auto-populated on persist — so these queries filter
    // directly rather than joining through trv_booking_services/trv_bookings
    // purely for org scoping.

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        Long orgId = SecurityHelper.requireOrgId();
        Map<String, Object> result = new LinkedHashMap<>();

        // ── Headline counts ───────────────────────────────────────────────────
        Map<String, Object> counts = jdbcTemplate.queryForMap("""
            SELECT
                COUNT(*) FILTER (WHERE status = 'DRAFT')                                  AS draft_count,
                COUNT(*) FILTER (WHERE status = 'CONFIRMED')                               AS confirmed_count,
                COUNT(*) FILTER (WHERE status IN ('CONFIRMED','PARTIALLY_PAID','PAID'))    AS active_count,
                COUNT(*) FILTER (WHERE status = 'CANCELLED')                               AS cancelled_count,
                COUNT(*) FILTER (WHERE status = 'COMPLETED')                               AS completed_count,
                COALESCE(SUM(total_amount) FILTER (WHERE status <> 'CANCELLED'), 0)        AS total_revenue,
                COALESCE(SUM(paid_amount)  FILTER (WHERE status <> 'CANCELLED'), 0)        AS total_collected,
                COALESCE(SUM(due_amount)   FILTER (WHERE status <> 'CANCELLED'), 0)        AS total_due,
                COUNT(*) FILTER (WHERE booking_date >= date_trunc('month', CURRENT_DATE))  AS bookings_this_month
            FROM trv_bookings WHERE organization_id = ?
            """, orgId);
        result.put("counts", counts);

        // ── Revenue trend, last 6 months ──────────────────────────────────────
        List<Map<String, Object>> revenueTrend = jdbcTemplate.queryForList("""
            SELECT TO_CHAR(month_start, 'Mon YYYY') AS month_label,
                   COALESCE(SUM(b.total_amount), 0) AS revenue,
                   COUNT(b.id)                      AS booking_count
            FROM   GENERATE_SERIES(
                       date_trunc('month', CURRENT_DATE) - INTERVAL '5 months',
                       date_trunc('month', CURRENT_DATE), INTERVAL '1 month'
                   ) AS month_start
            LEFT   JOIN trv_bookings b
                   ON  date_trunc('month', b.booking_date) = month_start
                   AND b.organization_id = ? AND b.status <> 'CANCELLED'
            GROUP  BY month_start ORDER BY month_start
            """, orgId);
        result.put("revenueTrend", revenueTrend);

        // ── Status breakdown (donut) ───────────────────────────────────────────
        List<Map<String, Object>> statusBreakdown = jdbcTemplate.queryForList("""
            SELECT status, COUNT(*) AS booking_count
            FROM   trv_bookings WHERE organization_id = ?
            GROUP  BY status ORDER BY booking_count DESC
            """, orgId);
        result.put("statusBreakdown", statusBreakdown);

        // ── Booking type breakdown ─────────────────────────────────────────────
        List<Map<String, Object>> typeBreakdown = jdbcTemplate.queryForList("""
            SELECT booking_type, COUNT(*) AS booking_count,
                   COALESCE(SUM(total_amount), 0) AS revenue
            FROM   trv_bookings WHERE organization_id = ? AND status <> 'CANCELLED'
            GROUP  BY booking_type ORDER BY revenue DESC
            """, orgId);
        result.put("typeBreakdown", typeBreakdown);

        // ── Top destinations — union across hotel / tour / package bookings ───
        List<Map<String, Object>> topDestinations = jdbcTemplate.queryForList("""
            SELECT destination, SUM(cnt) AS booking_count FROM (
                SELECT COALESCE(h.city, 'Unknown')  AS destination, COUNT(*) AS cnt
                FROM   trv_hotel_bookings hb JOIN trv_hotels h ON h.id = hb.hotel_id
                WHERE  hb.organization_id = ? GROUP BY h.city
                UNION ALL
                SELECT COALESCE(t.destination, 'Unknown') AS destination, COUNT(*) AS cnt
                FROM   trv_tour_bookings tb JOIN trv_tours t ON t.id = tb.tour_id
                WHERE  tb.organization_id = ? GROUP BY t.destination
                UNION ALL
                SELECT COALESCE(p.destination, 'Unknown') AS destination, COUNT(*) AS cnt
                FROM   trv_package_bookings pb JOIN trv_packages p ON p.id = pb.package_id
                WHERE  pb.organization_id = ? GROUP BY p.destination
            ) combined
            GROUP BY destination ORDER BY booking_count DESC LIMIT 6
            """, orgId, orgId, orgId);
        result.put("topDestinations", topDestinations);

        // ── Upcoming departures ────────────────────────────────────────────────
        List<Map<String, Object>> upcoming = jdbcTemplate.queryForList("""
            SELECT b.booking_no, b.travel_start_date,
                   COALESCE(s.sub_account_name, '—') AS customer_name, b.status
            FROM   trv_bookings b
            LEFT   JOIN acc_chart_of_accounts_sub s ON s.id = b.party_id
            WHERE  b.organization_id = ? AND b.status IN ('CONFIRMED','PARTIALLY_PAID','PAID')
              AND  b.travel_start_date >= CURRENT_DATE
            ORDER  BY b.travel_start_date ASC LIMIT 8
            """, orgId);
        result.put("upcomingDepartures", upcoming);

        // ── Top sales agents ────────────────────────────────────────────────────
        List<Map<String, Object>> topAgents = jdbcTemplate.queryForList("""
            SELECT COALESCE(u.full_name, u.username, 'Unassigned') AS agent_name,
                   COUNT(*) AS booking_count, COALESCE(SUM(b.total_amount),0) AS total_sales
            FROM   trv_bookings b
            LEFT   JOIN sec_users u ON u.id = b.sales_agent_id
            WHERE  b.organization_id = ? AND b.status <> 'CANCELLED'
            GROUP  BY agent_name ORDER BY total_sales DESC LIMIT 5
            """, orgId);
        result.put("topAgents", topAgents);

        // ── Flags — actionable items needing attention, severity-ranked ────────
        result.put("flags", buildDashboardFlags(orgId));

        return result;
    }

    /**
     * Travel-specific "needs attention" flags, same concept as the admin
     * command-center flag cards: severity ∈ critical | warning | info.
     */
    private List<Map<String, Object>> buildDashboardFlags(Long orgId) {
        List<Map<String, Object>> flags = new ArrayList<>();

        // Passports expiring within 6 months, for passengers on active bookings
        Long expiringPassports = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT p.id)
            FROM   trv_passengers p
            JOIN   trv_bookings b ON b.id = p.booking_id
            WHERE  b.organization_id = ? AND b.status IN ('CONFIRMED','PARTIALLY_PAID','PAID')
              AND  p.passport_expiry IS NOT NULL
              AND  p.passport_expiry BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '180 days'
            """, Long.class, orgId);
        flags.add(flag("critical", "fa-passport", "Passports Expiring Soon",
            expiringPassports, "within 6 months, on active bookings", "/travel/bookings"));

        // Visa applications close to their expected date but not yet approved/collected
        Long visaAtRisk = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM trv_visa_applications
            WHERE  organization_id = ? AND status NOT IN ('APPROVED','COLLECTED','REJECTED')
              AND  expected_date IS NOT NULL AND expected_date <= CURRENT_DATE + INTERVAL '3 days'
            """, Long.class, orgId);
        flags.add(flag("critical", "fa-clock", "Visa At Risk",
            visaAtRisk, "expected within 3 days, not yet approved", "/travel/visa-applications"));

        // Confirmed bookings departing within 7 days with an AIR line but no ticket issued
        Long unissuedTickets = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT bs.id)
            FROM   trv_booking_services bs
            JOIN   trv_bookings b ON b.id = bs.booking_id
            WHERE  b.organization_id = ? AND bs.service_type = 'AIR'
              AND  b.status IN ('CONFIRMED','PARTIALLY_PAID','PAID')
              AND  b.travel_start_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
              AND  NOT EXISTS (SELECT 1 FROM trv_air_tickets at WHERE at.booking_service_id = bs.id)
            """, Long.class, orgId);
        flags.add(flag("critical", "fa-plane-slash", "Air Tickets Not Issued",
            unissuedTickets, "departing within 7 days", "/travel/air-tickets"));

        // Hotel bookings still PENDING with check-in within 7 days
        Long pendingHotels = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM trv_hotel_bookings
            WHERE  organization_id = ? AND status = 'PENDING'
              AND  check_in_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
            """, Long.class, orgId);
        flags.add(flag("warning", "fa-bed", "Hotel Bookings Unconfirmed",
            pendingHotels, "check-in within 7 days", "/travel/hotel-bookings"));

        // Unpaid supplier costs
        Map<String, Object> unpaidSuppliers = jdbcTemplate.queryForMap("""
            SELECT COUNT(*) AS cnt, COALESCE(SUM(cost_amount), 0) AS total
            FROM   trv_supplier_costs
            WHERE  organization_id = ? AND payment_status = 'UNPAID'
            """, orgId);
        flags.add(flag("warning", "fa-file-invoice-dollar", "Unpaid Supplier Costs",
            unpaidSuppliers.get("cnt"),
            "৳" + unpaidSuppliers.get("total") + " outstanding", "/travel/supplier-costs"));

        // Overdue receivables — travel already started but balance still due
        Long overdueReceivables = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM trv_bookings
            WHERE  organization_id = ? AND status IN ('CONFIRMED','PARTIALLY_PAID')
              AND  due_amount > 0 AND travel_start_date IS NOT NULL AND travel_start_date < CURRENT_DATE
            """, Long.class, orgId);
        flags.add(flag("warning", "fa-money-bill-wave", "Overdue Receivables",
            overdueReceivables, "travel started, balance still due", "/travel/bookings"));

        // Draft bookings with imminent travel dates — at risk of falling through
        Long staleDrafts = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM trv_bookings
            WHERE  organization_id = ? AND status = 'DRAFT'
              AND  travel_start_date IS NOT NULL
              AND  travel_start_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '5 days'
            """, Long.class, orgId);
        flags.add(flag("warning", "fa-hourglass-half", "Unconfirmed Drafts, Travel Imminent",
            staleDrafts, "travel within 5 days, still DRAFT", "/travel/bookings"));

        // Good news — active tours/packages this month, as a positive signal
        Long activeThisMonth = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM trv_bookings
            WHERE  organization_id = ? AND status IN ('CONFIRMED','PARTIALLY_PAID','PAID')
              AND  travel_start_date >= date_trunc('month', CURRENT_DATE)
              AND  travel_start_date <  date_trunc('month', CURRENT_DATE) + INTERVAL '1 month'
            """, Long.class, orgId);
        flags.add(flag("good", "fa-plane-departure", "Departures This Month",
            activeThisMonth, "confirmed and on schedule", "/travel/dashboard"));

        return flags;
    }

    private Map<String, Object> flag(String severity, String icon, String label,
                                      Object value, String sub, String url) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("severity", severity);
        f.put("icon", icon);
        f.put("label", label);
        f.put("value", value);
        f.put("sub", sub);
        f.put("url", url);
        return f;
    }

    // =========================================================================
    // MAPPING  entity → DTO
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public TrvBookingDTO toDTO(TrvBooking e) {
        TrvBookingDTO d = TrvBookingDTO.builder()
            .id(e.getId())
            .bookingNo(e.getBookingNo())
            .bookingType(e.getBookingType() != null ? e.getBookingType().name() : null)
            .bookingDate(e.getBookingDate())
            .travelStartDate(e.getTravelStartDate())
            .travelEndDate(e.getTravelEndDate())
            .status(e.getStatus() != null ? e.getStatus().name() : null)
            .currency(e.getCurrency())
            .exchangeRate(e.getExchangeRate())
            .subtotalAmount(e.getSubtotalAmount())
            .discountAmount(e.getDiscountAmount())
            .taxAmount(e.getTaxAmount())
            .totalAmount(e.getTotalAmount())
            .paidAmount(e.getPaidAmount())
            .dueAmount(e.getDueAmount())
            .remarks(e.getRemarks())
            .partyId(e.getPartyId())
            .leadId(e.getLeadId())
            .opportunityId(e.getOpportunityId())
            .salesAgentId(e.getSalesAgentId())
            .approvalRequestId(e.getApprovalRequestId())
            .journalEntryId(e.getJournalEntryId())
            .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
            .createdBy(e.getCreatedBy())
            .build();

        if (e.getPartyId() != null) {
            subRepo.findById(e.getPartyId()).ifPresent(s ->
                d.setPartyDisplay(s.getSubAccountCode() + " — " + s.getSubAccountName()));
        }

        d.setServices(e.getServices().stream().map(s -> TrvBookingDTO.ServiceLineDTO.builder()
            .id(s.getId())
            .serviceType(s.getServiceType() != null ? s.getServiceType().name() : null)
            .referenceId(s.getReferenceId())
            .description(s.getDescription())
            .quantity(s.getQuantity())
            .unitCost(s.getUnitCost())
            .unitPrice(s.getUnitPrice())
            .discountAmount(s.getDiscountAmount())
            .taxAmount(s.getTaxAmount())
            .lineTotal(s.getLineTotal())
            .costCenterId(s.getCostCenterId())
            .build()).collect(Collectors.toList()));

        d.setPassengers(e.getPassengers().stream().map(p -> {
            TrvBookingDTO.PassengerDTO pd = TrvBookingDTO.PassengerDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .passportNumber(p.getPassportNumber())
                .passportExpiry(p.getPassportExpiry())
                .nationality(p.getNationality())
                .passengerType(p.getPassengerType() != null ? p.getPassengerType().name() : "ADULT")
                .isLeadPassenger(p.getIsLeadPassenger())
                .phone(p.getPhone())
                .email(p.getEmail())
                .remarks(p.getRemarks())
                .build();
            preferenceRepo.findByPassengerId(p.getId()).ifPresent(pref ->
                pd.setPreference(TrvBookingDTO.PreferenceDTO.builder()
                    .id(pref.getId())
                    .mealPreference(pref.getMealPreference())
                    .seatPreference(pref.getSeatPreference())
                    .specialAssistance(pref.getSpecialAssistance())
                    .dietaryRestriction(pref.getDietaryRestriction())
                    .remarks(pref.getRemarks())
                    .build()));
            return pd;
        }).collect(Collectors.toList()));

        return d;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void buildHeader(TrvBookingDTO dto, TrvBooking e) {
        Long orgId = SecurityHelper.requireOrgId();
        e.setBookingType(TrvBooking.BookingType.valueOf(dto.getBookingType()));
        e.setBookingDate(dto.getBookingDate());
        e.setTravelStartDate(dto.getTravelStartDate());
        e.setTravelEndDate(dto.getTravelEndDate());
        e.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "BDT");
        e.setExchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate() : BigDecimal.ONE);
        e.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO);
        e.setRemarks(dto.getRemarks());
        e.setPartyId(dto.getPartyId());
        e.setLeadId(dto.getLeadId());
        e.setOpportunityId(dto.getOpportunityId());
        e.setSalesAgentId(dto.getSalesAgentId());
//        if (e.getOrganizationId() == null) e.setOrganizationId(orgId);

        String user = SecurityHelper.currentUsername().orElse("system");
        if (e.getCreatedBy() == null) e.setCreatedBy(user);
        e.setUpdatedBy(user);
        if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        if (e.getBookingNo() == null || e.getBookingNo().isBlank()) {
            String year = String.valueOf(LocalDate.now().getYear()).substring(2);
            e.setBookingNo(seqService.nextDocumentNumber(orgId, "BKG", year));
        }
    }

    private void syncServices(TrvBookingDTO dto, TrvBooking parent) {
        parent.getServices().clear();
        if (dto.getServices() == null) return;
        for (TrvBookingDTO.ServiceLineDTO sd : dto.getServices()) {
            if (sd.getServiceType() == null) continue;
            parent.getServices().add(TrvBookingService.builder()
                .serviceType(TrvBookingService.ServiceType.valueOf(sd.getServiceType()))
                .referenceId(sd.getReferenceId())
                .description(sd.getDescription())
                .quantity(sd.getQuantity() != null ? sd.getQuantity() : BigDecimal.ONE)
                .unitCost(sd.getUnitCost() != null ? sd.getUnitCost() : BigDecimal.ZERO)
                .unitPrice(sd.getUnitPrice() != null ? sd.getUnitPrice() : BigDecimal.ZERO)
                .discountAmount(sd.getDiscountAmount() != null ? sd.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(sd.getTaxAmount() != null ? sd.getTaxAmount() : BigDecimal.ZERO)
                .lineTotal(sd.getLineTotal() != null ? sd.getLineTotal() : BigDecimal.ZERO)
                .costCenterId(sd.getCostCenterId())
                .booking(parent)
                .build());
        }
    }

    private void syncPassengers(TrvBookingDTO dto, TrvBooking parent) {
        parent.getPassengers().clear();
        if (dto.getPassengers() == null) return;
        for (TrvBookingDTO.PassengerDTO pd : dto.getPassengers()) {
            if (pd.getFirstName() == null || pd.getFirstName().isBlank()) continue;
            TrvPassenger passenger = TrvPassenger.builder()
                .title(pd.getTitle())
                .firstName(pd.getFirstName())
                .lastName(pd.getLastName())
                .dateOfBirth(pd.getDateOfBirth())
                .gender(pd.getGender() != null ? TrvPassenger.Gender.valueOf(pd.getGender()) : null)
                .passportNumber(pd.getPassportNumber())
                .passportExpiry(pd.getPassportExpiry())
                .nationality(pd.getNationality())
                .passengerType(pd.getPassengerType() != null
                    ? TrvPassenger.PassengerType.valueOf(pd.getPassengerType()) : TrvPassenger.PassengerType.ADULT)
                .isLeadPassenger(Boolean.TRUE.equals(pd.getIsLeadPassenger()))
                .phone(pd.getPhone())
                .email(pd.getEmail())
                .remarks(pd.getRemarks())
                .booking(parent)
                .build();
            parent.getPassengers().add(passenger);
        }
    }

    /** Persists preferences after passengers get their generated IDs (post-save pass). */
    private void syncPreferencesAfterSave(TrvBookingDTO dto, TrvBooking saved) {
        if (dto.getPassengers() == null) return;
        List<TrvPassenger> savedPax = saved.getPassengers();
        for (int i = 0; i < dto.getPassengers().size() && i < savedPax.size(); i++) {
            TrvBookingDTO.PreferenceDTO pref = dto.getPassengers().get(i).getPreference();
            if (pref == null) continue;
            Long paxId = savedPax.get(i).getId();
            TrvPassengerPreference entity = preferenceRepo.findByPassengerId(paxId)
                .orElse(TrvPassengerPreference.builder().passengerId(paxId).build());
            entity.setMealPreference(pref.getMealPreference());
            entity.setSeatPreference(pref.getSeatPreference());
            entity.setSpecialAssistance(pref.getSpecialAssistance());
            entity.setDietaryRestriction(pref.getDietaryRestriction());
            entity.setRemarks(pref.getRemarks());
            preferenceRepo.save(entity);
        }
    }

    private void recalcTotals(TrvBooking e) {
        BigDecimal subtotal = e.getServices().stream()
            .map(s -> s.getLineTotal() != null ? s.getLineTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = e.getServices().stream()
            .map(s -> s.getTaxAmount() != null ? s.getTaxAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = e.getDiscountAmount() != null ? e.getDiscountAmount() : BigDecimal.ZERO;

        e.setSubtotalAmount(subtotal);
        e.setTaxAmount(tax);
        BigDecimal total = subtotal.subtract(discount).add(tax);
        e.setTotalAmount(total);
        BigDecimal paid = e.getPaidAmount() != null ? e.getPaidAmount() : BigDecimal.ZERO;
        e.setDueAmount(total.subtract(paid));
    }

    private void logStatus(TrvBooking booking, String status, String remarks) {
        historyRepo.save(TrvBookingStatusHistory.builder()
            .bookingId(booking.getId())
            .status(status)
            .changedBy(SecurityHelper.currentUsername().orElse("system"))
            .remarks(remarks)
            .build());
    }

    private TrvBooking findBooking(Long id) {
        return bookingRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking #" + id + " not found."));
    }

    private void guardDraft(TrvBooking booking) {
        if (booking.getStatus() != TrvBooking.Status.DRAFT)
            throw new IllegalStateException(
                "Booking " + booking.getBookingNo() + " is " + booking.getStatus() + ". Only DRAFT can be modified.");
    }
}
