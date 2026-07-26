package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * DROP-IN REPLACEMENT — the original findByBookingId is untouched; everything
 * below it is additive.
 */
public interface TrvPassengerRepository extends JpaRepository<TrvPassenger, Long> {

    List<TrvPassenger> findByBookingId(Long bookingId);

    // ── Added for standalone passenger CRUD ──────────────────────────────────

    /** Tenant-safe fetch — never load a passenger belonging to another org. */
    @Query("""
           SELECT p FROM TrvPassenger p
           WHERE  p.id = :id AND p.booking.organization.id = :orgId
           """)
    Optional<TrvPassenger> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    /**
     * Duplicate guard: the same passport number already on the same booking.
     *
     * excludeId is a primitive on purpose — pass -1 for "nothing to exclude"
     * rather than null. A nullable bind parameter inside an IS NULL comparison
     * makes Hibernate's type inference brittle, and -1 can never be a real id.
     */
    @Query("""
           SELECT COUNT(p) FROM TrvPassenger p
           WHERE  p.booking.id = :bookingId
             AND  UPPER(p.passportNumber) = UPPER(:passportNumber)
             AND  p.id <> :excludeId
           """)
    long countDuplicatePassport(@Param("bookingId") Long bookingId,
                                @Param("passportNumber") String passportNumber,
                                @Param("excludeId") long excludeId);

    /** Clears the lead flag on every other passenger of the booking. */
    @Query("""
           SELECT p FROM TrvPassenger p
           WHERE  p.booking.id = :bookingId AND p.isLeadPassenger = true AND p.id <> :keepId
           """)
    List<TrvPassenger> findOtherLeads(@Param("bookingId") Long bookingId,
                                      @Param("keepId") Long keepId);
}
